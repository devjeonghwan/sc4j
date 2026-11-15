package org.fogofwarlabs.sc4j.launcher.ui;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.shape.*;
import org.jetbrains.annotations.NotNull;

/**
 * Gives the chamfered regions of the launcher theme an outline whose 45-degree cuts keep their size in pixels at every width and height.
 * <p>
 * JavaFX fits a {@code -fx-shape} path to the size of its region, so a cut drawn into the path would grow and shrink with the region and
 * change its angle. The stylesheet gives the cuts as sizes instead: a region whose stylesheet turns off both {@code -fx-scale-shape} and
 * {@code -fx-position-shape} and gives its background corner radii is chamfered, each radius being the cut at that corner in pixels, such as
 * {@code -fx-background-radius: 6 0 6 0} for the top-left and bottom-right corners. Whenever the size, background or border of such a region
 * changes, it gets a polygon of exactly its size with those cuts as its shape, which JavaFX draws as it is.
 * <p>
 * A background fill without insets fills the polygon, and a border stroke runs inside along it, so the edge is as wide along a cut as along
 * the sides; a fill with insets would be a scaled copy of the polygon, so the stylesheet draws the edge of a chamfered region as its border.
 * A border without a bottom width leaves the bottom side open, as on a tab whose bottom joins the content below it. A cut is at most half the
 * shorter side of the region.
 * <p>
 * {@link ThemeUtils} installs this on every themed scene and dialog; it covers the regions that skins create too, such as tab headers and
 * the buttons of a dialog.
 */
public final class ChamferUtils {
    /**
     * Marks a parent whose children are watched.
     */
    private static final @NotNull String INSTALLED_KEY = "sc4j.chamfer.installed";

    /**
     * Marks a region with a cut, whose size, border and shape settings are watched as well as its background.
     */
    private static final @NotNull String WATCHED_KEY = "sc4j.chamfer.watched";

    private static final @NotNull ListChangeListener<Node> CHILDREN_LISTENER = change -> {
        while (change.next()) {
            for (Node child : change.getAddedSubList()) {
                install(child);
            }
        }
    };
    private static final @NotNull InvalidationListener REGION_LISTENER = ChamferUtils::onRegionChanged;

    /**
     * The polygon of one region size: the region's rectangle with its corners cut.
     */
    private static final class Outline extends Path {
        private final double width;
        private final double height;
        private final @NotNull CornerRadii cuts;
        private final boolean bottomOpen;

        private Outline(double width, double height, @NotNull CornerRadii cuts, boolean bottomOpen) {
            double limit = Math.min(width, height) / 2;
            double topLeft = Math.min(cuts.getTopLeftHorizontalRadius(), limit);
            double topRight = Math.min(cuts.getTopRightHorizontalRadius(), limit);
            double bottomRight = Math.min(cuts.getBottomRightHorizontalRadius(), limit);
            double bottomLeft = Math.min(cuts.getBottomLeftHorizontalRadius(), limit);

            this.width      = width;
            this.height     = height;
            this.cuts       = cuts;
            this.bottomOpen = bottomOpen;

            // Clockwise from the left end of the bottom side, so that the bottom side is the closing segment that an open outline leaves out.
            this.getElements().add(new MoveTo(bottomLeft, height));

            if (bottomLeft > 0) {
                this.getElements().add(new LineTo(0, height - bottomLeft));
            }

            this.getElements().add(new LineTo(0, topLeft));

            if (topLeft > 0) {
                this.getElements().add(new LineTo(topLeft, 0));
            }

            this.getElements().add(new LineTo(width - topRight, 0));

            if (topRight > 0) {
                this.getElements().add(new LineTo(width, topRight));
            }

            this.getElements().add(new LineTo(width, height - bottomRight));

            if (bottomRight > 0) {
                this.getElements().add(new LineTo(width - bottomRight, height));
            }

            if (!bottomOpen) {
                this.getElements().add(new ClosePath());
            }
        }

        private boolean matches(double width, double height, @NotNull CornerRadii cuts, boolean bottomOpen) {
            return this.width == width && this.height == height && this.cuts.equals(cuts) && this.bottomOpen == bottomOpen;
        }
    }

    /**
     * Watches the regions of a scene, whatever its root is now or later, and gives the chamfered ones their outline.
     *
     * @param scene the scene
     */
    public static void install(@NotNull Scene scene) {
        if (scene.getProperties().putIfAbsent(INSTALLED_KEY, Boolean.TRUE) != null) {
            return;
        }

        scene.rootProperty().addListener((observable, previous, root) -> install(root));
        install(scene.getRoot());
    }

    /**
     * Watches the regions of a node and of the children it has now or later, and gives the chamfered ones their outline.
     *
     * @param node the node
     */
    public static void install(@NotNull Node node) {
        if (!(node instanceof Parent parent) || parent.getProperties().putIfAbsent(INSTALLED_KEY, Boolean.TRUE) != null) {
            return;
        }

        if (parent instanceof Region region) {
            region.backgroundProperty().addListener(REGION_LISTENER);
            update(region);
        }

        parent.getChildrenUnmodifiable().addListener(CHILDREN_LISTENER);

        for (Node child : parent.getChildrenUnmodifiable()) {
            install(child);
        }
    }

    private static void onRegionChanged(@NotNull Observable observable) {
        update((Region) ((ReadOnlyProperty<?>) observable).getBean());
    }

    /**
     * Gives a region the outline of its size and cuts, or takes its outline away when it has no cut any more. Every region property this
     * reads is read on each call, so that its invalidation listener hears the next change.
     */
    private static void update(@NotNull Region region) {
        Background background = region.getBackground();
        Border border = region.getBorder();
        double width = region.getWidth();
        double height = region.getHeight();
        boolean unscaled = !region.isScaleShape() && !region.isCenterShape();
        Shape shape = region.getShape();
        CornerRadii cuts = background == null || background.getFills().isEmpty() ? CornerRadii.EMPTY : background.getFills().getFirst().getRadii();
        boolean hasCut = cuts.getTopLeftHorizontalRadius() > 0 || cuts.getTopRightHorizontalRadius() > 0 ||
                         cuts.getBottomRightHorizontalRadius() > 0 || cuts.getBottomLeftHorizontalRadius() > 0;
        boolean bottomOpen;

        if (!region.getProperties().containsKey(WATCHED_KEY)) {
            if (!hasCut) {
                return;
            }

            region.getProperties().put(WATCHED_KEY, Boolean.TRUE);
            region.widthProperty().addListener(REGION_LISTENER);
            region.heightProperty().addListener(REGION_LISTENER);
            region.borderProperty().addListener(REGION_LISTENER);
            region.scaleShapeProperty().addListener(REGION_LISTENER);
            region.centerShapeProperty().addListener(REGION_LISTENER);
        }

        if (!hasCut || !unscaled) {
            if (shape instanceof Outline) {
                region.setShape(null);
            }

            return;
        }

        if (width <= 0 || height <= 0) {
            return;
        }

        bottomOpen = border != null && !border.getStrokes().isEmpty() && border.getStrokes().getFirst().getWidths().getBottom() == 0;

        if (!(shape instanceof Outline outline) || !outline.matches(width, height, cuts, bottomOpen)) {
            region.setShape(new Outline(width, height, cuts, bottomOpen));
        }
    }


    private ChamferUtils() {
    }
}
