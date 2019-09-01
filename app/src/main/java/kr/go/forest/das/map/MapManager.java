package kr.go.forest.das.map;

import android.util.Log;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.MapTileIndex;

public class MapManager {

    public static final OnlineTileSourceBase VWorldStreet;
    public static final OnlineTileSourceBase RestrictedArea;
    public static final OnlineTileSourceBase ProhibitededArea;

    private static final double MAP_SIZE = 20037508.34789244 * 2;
    private static final int ORIG_X = 0;
    private static final int ORIG_Y = 1;
    protected static final int MINX = 0;
    protected static final int MAXX = 1;
    protected static final int MINY = 2;
    protected static final int MAXY = 3;

    static {
        VWorldStreet = new OnlineTileSourceBase("VWorld", 0, 22, 256, "jpeg",
                new String[0], "VWorld") {

            public String getTileURLString(final long pMapTileIndex) {
                Log.e("Image", "" + MapTileIndex.getZoom(pMapTileIndex) + " " + MapTileIndex.getX(pMapTileIndex) + " " + MapTileIndex.getY(pMapTileIndex));
                return "http://xdworld.vworld.kr:8080/2d/Satellite/service/"+ MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + ".jpeg";
            }
        };

        RestrictedArea = new OnlineTileSourceBase("VWorldLayer", 0, 22, 512, "png",
                new String[0], "VWorldLayer") {

            public String getTileURLString(final long pMapTileIndex) {
                double[] bbox = getBoundingBox(MapTileIndex.getX(pMapTileIndex), MapTileIndex.getY(pMapTileIndex), MapTileIndex.getZoom(pMapTileIndex));

                return "http://2d.vworld.kr:8895/2DCache/gis/map/WMS2?LAYERS=LT_C_AISRESC&STYLES=LT_C_AISRESC&CRS=EPSG:900913&BBOX="
                        + bbox[MINX] +"," + bbox[MINY] + "," + bbox[MAXX] + "," + bbox[MAXY] + "&SIZE=512&APIKEY=767B7ADF-10BA-3D86-AB7E-02816B5B92E9";
            }
        };

        ProhibitededArea = new OnlineTileSourceBase("VWorldLayer", 0, 22, 512, "png",
                new String[0], "VWorldLayer") {

            public String getTileURLString(final long pMapTileIndex) {
                double[] bbox = getBoundingBox(MapTileIndex.getX(pMapTileIndex), MapTileIndex.getY(pMapTileIndex), MapTileIndex.getZoom(pMapTileIndex));

                return "http://2d.vworld.kr:8895/2DCache/gis/map/WMS2?LAYERS=LT_C_AISPRHC&STYLES=LT_C_AISPRHC&CRS=EPSG:900913&BBOX="
                        + bbox[MINX] +"," + bbox[MINY] + "," + bbox[MAXX] + "," + bbox[MAXY] + "&SIZE=512&APIKEY=767B7ADF-10BA-3D86-AB7E-02816B5B92E9";
            }
        };
    }

    public static double[] getBoundingBox(int x, int y, int zoom) {

        double[] TILE_ORIGIN = {-20037508.34789244, 20037508.34789244};

        double tileSize = MAP_SIZE / Math.pow(2, zoom);
        double minx = TILE_ORIGIN[ORIG_X] + x * tileSize;
        double maxx = TILE_ORIGIN[ORIG_X] + (x + 1) * tileSize;
        double miny = TILE_ORIGIN[ORIG_Y] - (y + 1) * tileSize;
        double maxy = TILE_ORIGIN[ORIG_Y] - y * tileSize;

        double[] bbox = new double[4];
        bbox[MINX] = minx;
        bbox[MINY] = miny;
        bbox[MAXX] = maxx;
        bbox[MAXY] = maxy;

        return bbox;
    }

    // V-World 비행제한 구역 레이어
    public  OnlineTileSourceBase GetRestrictedAreaSource()
    {
        OnlineTileSourceBase _VWorldWMS = new OnlineTileSourceBase("VWorldLayer", 0, 22, 512, "png",
                new String[0], "VWorldLayer")
        {
            private final double MAP_SIZE = 20037508.34789244 * 2;
            private final int ORIG_X = 0;
            private final int ORIG_Y = 1;
            protected final int MIN_X = 0;
            protected final int MAX_X = 1;
            protected final int MIN_Y = 2;
            protected final int MAX_Y = 3;
            private  String layer = "";

            public String getTileURLString(final long pMapTileIndex) {
                double[] bbox = getBoundingBox(MapTileIndex.getX(pMapTileIndex), MapTileIndex.getY(pMapTileIndex), MapTileIndex.getZoom(pMapTileIndex));

                return "http://2d.vworld.kr:8895/2DCache/gis/map/WMS2?LAYERS=LT_C_AISRESC&STYLES=LT_C_AISRESC&CRS=EPSG:900913&BBOX="
                        + bbox[MIN_X] +"," + bbox[MIN_Y] + "," + bbox[MAX_X] + "," + bbox[MAX_Y] + "&SIZE=512&APIKEY=767B7ADF-10BA-3D86-AB7E-02816B5B92E9";
            }

            public  double[] getBoundingBox(int x, int y, int zoom) {

                double[] TILE_ORIGIN = {-20037508.34789244, 20037508.34789244};

                double tileSize = MAP_SIZE / Math.pow(2, zoom);
                double minx = TILE_ORIGIN[ORIG_X] + x * tileSize;
                double maxx = TILE_ORIGIN[ORIG_X] + (x + 1) * tileSize;
                double miny = TILE_ORIGIN[ORIG_Y] - (y + 1) * tileSize;
                double maxy = TILE_ORIGIN[ORIG_Y] - y * tileSize;

                double[] bbox = new double[4];
                bbox[MIN_X] = minx;
                bbox[MIN_Y] = miny;
                bbox[MAX_X] = maxx;
                bbox[MAX_Y] = maxy;

                return bbox;
            }
        };

        return _VWorldWMS;
    }

    // V-World 비행금지 구역 레이어
    public  OnlineTileSourceBase GetProhibitededAreaSource()
    {
        OnlineTileSourceBase _VWorldWMS = new OnlineTileSourceBase("VWorldLayer", 0, 22, 512, "png",
                new String[0], "VWorldLayer")
        {
            private final double MAP_SIZE = 20037508.34789244 * 2;
            private final int ORIG_X = 0;
            private final int ORIG_Y = 1;
            protected final int MIN_X = 0;
            protected final int MAX_X = 1;
            protected final int MIN_Y = 2;
            protected final int MAX_Y = 3;
            private  String layer = "";

            public String getTileURLString(final long pMapTileIndex) {
                double[] bbox = getBoundingBox(MapTileIndex.getX(pMapTileIndex), MapTileIndex.getY(pMapTileIndex), MapTileIndex.getZoom(pMapTileIndex));

                return "http://2d.vworld.kr:8895/2DCache/gis/map/WMS2?LAYERS=LT_C_AISPRHC&STYLES=LT_C_AISPRHC&CRS=EPSG:900913&BBOX="
                        + bbox[MIN_X] +"," + bbox[MIN_Y] + "," + bbox[MAX_X] + "," + bbox[MAX_Y] + "&SIZE=512&APIKEY=767B7ADF-10BA-3D86-AB7E-02816B5B92E9";
            }

            public  double[] getBoundingBox(int x, int y, int zoom) {

                double[] TILE_ORIGIN = {-20037508.34789244, 20037508.34789244};

                double tileSize = MAP_SIZE / Math.pow(2, zoom);
                double minx = TILE_ORIGIN[ORIG_X] + x * tileSize;
                double maxx = TILE_ORIGIN[ORIG_X] + (x + 1) * tileSize;
                double miny = TILE_ORIGIN[ORIG_Y] - (y + 1) * tileSize;
                double maxy = TILE_ORIGIN[ORIG_Y] - y * tileSize;

                double[] bbox = new double[4];
                bbox[MIN_X] = minx;
                bbox[MIN_Y] = miny;
                bbox[MAX_X] = maxx;
                bbox[MAX_Y] = maxy;

                return bbox;
            }
        };

        return _VWorldWMS;
    }
}
