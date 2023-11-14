package net.fabricmc.example;

public class PlayerLimit {
    public int chunkX;
    public int chunkZ;
    public int chunkMaxX;
    public int chunkMaxZ;

    public PlayerLimit(int x, int z)
    {
        this.chunkX = x - 1;
        this.chunkZ = z - 1;
        this.chunkMaxX = x + 18;
        this.chunkMaxZ = z + 18;
    }

    public PlayerLimit getNether(){
        return new PlayerLimit((((chunkX + 1) / 8) / 16) * 16, (((chunkZ + 1) / 8) / 16) * 16);
    }

    @Override
    public String toString() {
        return "PlayerLimit{" +
                "chunkX=" + chunkX +
                ", chunkY=" + chunkZ +
                '}';
    }
}
