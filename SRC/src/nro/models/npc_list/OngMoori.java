package nro.models.npc_list;

public class OngMoori extends OngGohan {
    public OngMoori(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    protected String getHomeGreeting() {
        return "Con cố gắng theo Trưởng Lão Guru học thành tài, đừng lo lắng cho ta.";
    }
}
