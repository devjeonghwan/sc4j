package org.fogofwarlabs.sc4j.beaconlink.peer.account;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds an account in use: every process that uses an account (a game, the launcher's session) holds a shared lock of the account's lock
 * file ({@value Account#LOCK_FILE}) while it uses it. Using an account never excludes another user of it, of this process or another one;
 * the lock only keeps the account from being deleted meanwhile ({@link AccountStore#delete}), which needs the exclusive lock. The operating
 * system releases the lock of a process that ends, so a crashed game never leaves its account in use.
 * <p>
 * A JVM refuses two overlapping locks of one file, so one JVM holds one shared lock per account and counts its users; the last user to close
 * releases it.
 */
public final class AccountLock implements Closeable {
    private static final long WHOLE_FILE = Long.MAX_VALUE;
    private static final @NotNull Map<@NotNull Path, @NotNull Holder> HOLDERS = new HashMap<>();

    /**
     * The shared lock this JVM holds on one account's lock file and the number of its open users.
     */
    private static final class Holder {
        private final @NotNull FileChannel channel;
        private final @NotNull FileLock lock;

        private int users;

        private Holder(@NotNull FileChannel channel, @NotNull FileLock lock) {
            this.channel = channel;
            this.lock    = lock;

            this.users = 0;
        }
    }

    /**
     * Starts a use of an account: takes the shared lock of its lock file, or counts one more user of the lock this JVM holds.
     *
     * @param account the account
     * @return the use, held until it is closed or the process ends
     * @throws AccountException if the account is being deleted ({@link AccountProblem#IN_USE})
     * @throws IOException      if the lock file cannot be created or opened
     */
    public static synchronized @NotNull AccountLock acquire(@NotNull Account account) throws IOException {
        Path file = getLockFile(account);
        Holder holder = HOLDERS.get(file);

        if (holder == null) {
            FileChannel channel = open(file);
            FileLock lock;

            try {
                lock = channel.tryLock(0L, WHOLE_FILE, true);
            } catch (OverlappingFileLockException exception) {
                // This process deletes the account.
                lock = null;
            } catch (IOException | RuntimeException exception) {
                IgnoreException.execute(
                        channel::close
                );

                throw exception;
            }

            if (lock == null) {
                channel.close();

                throw new AccountException(AccountProblem.IN_USE, Exceptions.format("The account {0} is being deleted.", account.getId()));
            }

            holder = new Holder(channel, lock);
            HOLDERS.put(file, holder);
        }

        holder.users++;

        return new AccountLock(account, file, holder);
    }

    /**
     * Takes the exclusive lock of an account's lock file if no process uses the account, this one included.
     *
     * @param account the account
     * @return the open lock file holding the exclusive lock, which closing releases, or {@code null} if a process uses the account
     * @throws IOException if the lock file cannot be created or opened
     */
    static synchronized @Nullable FileChannel lockExclusively(@NotNull Account account) throws IOException {
        Path file = getLockFile(account);

        if (HOLDERS.containsKey(file)) {
            return null;
        }

        FileChannel channel = open(file);
        FileLock lock;

        try {
            lock = channel.tryLock(0L, WHOLE_FILE, false);
        } catch (OverlappingFileLockException exception) {
            // This process checks or deletes the account already.
            lock = null;
        } catch (IOException | RuntimeException exception) {
            IgnoreException.execute(
                    channel::close
            );

            throw exception;
        }

        if (lock == null) {
            channel.close();

            return null;
        }

        return channel;
    }

    private static @NotNull Path getLockFile(@NotNull Account account) {
        return account.getDirectory().resolve(Account.LOCK_FILE).toAbsolutePath().normalize();
    }

    private static @NotNull FileChannel open(@NotNull Path file) throws IOException {
        return FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
    }

    private static synchronized void release(@NotNull Path file, @NotNull Holder holder) {
        holder.users--;

        if (holder.users == 0) {
            HOLDERS.remove(file);
            IgnoreException.execute(
                    holder.lock::release,
                    holder.channel::close
            );
        }
    }


    private final @NotNull Account account;
    private final @NotNull Path file;
    private final @NotNull Holder holder;

    private boolean closed;

    private AccountLock(@NotNull Account account, @NotNull Path file, @NotNull Holder holder) {
        this.account = account;
        this.file    = file;
        this.holder  = holder;

        this.closed = false;
    }

    /**
     * @return the account in use
     */
    public @NotNull Account getAccount() {
        return this.account;
    }

    /**
     * Ends this use of the account; the lock is released when no other use of this JVM is open. Closing twice does nothing.
     */
    @Override
    public void close() {
        synchronized (AccountLock.class) {
            if (!this.closed) {
                this.closed = true;
                release(this.file, this.holder);
            }
        }
    }
}
