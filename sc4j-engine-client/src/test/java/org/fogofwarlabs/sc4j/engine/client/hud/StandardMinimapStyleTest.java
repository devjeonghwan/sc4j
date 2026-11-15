package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.world.FogLayer;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests the default minimap look: team-coloured dots, the world view's fog colours, a white camera outline and a ping that shrinks.
 */
final class StandardMinimapStyleTest {
    private static final int FRAMES = 10;
    private static final int OPAQUE_ALPHA = 0xFF;

    @Test
    void standard_minimap_style_uses_team_colours_and_shrinks_pings() throws IOException {
        GameView view = new SkirmishSessionScenario().getSession().getView();
        StandardMinimapStyle style = new StandardMinimapStyle();
        Unit unit = view.getUnits().stream().filter(candidate -> view.getUnitView(candidate).getOwner() != null).findFirst().orElseThrow();
        Player owner = view.getUnitView(unit).getOwner();

        Assertions.assertNotNull(owner);
        Assertions.assertEquals(view.getTeamColor(owner) << Byte.SIZE | OPAQUE_ALPHA, style.getUnitColor(view, unit, owner));
        Assertions.assertEquals(FogLayer.DEFAULT_FOG_COLOR, style.getFogColor(true));
        Assertions.assertEquals(FogLayer.DEFAULT_UNEXPLORED_COLOR, style.getFogColor(false));
        Assertions.assertEquals(StandardMinimapStyle.CAMERA_COLOR, style.getCameraColor());
        Assertions.assertEquals(StandardMinimapStyle.PING_START_SIZE, style.getPingSize(0, FRAMES));
        Assertions.assertEquals(StandardMinimapStyle.PING_END_SIZE, style.getPingSize(FRAMES - 1, FRAMES));
        Assertions.assertTrue(style.getPingSize(1, FRAMES) < StandardMinimapStyle.PING_START_SIZE);
    }
}
