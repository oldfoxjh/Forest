package kr.go.forest.das;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;

import kr.go.forest.das.map.MapManager;

public class MissionActivity extends Activity {

    private MapView mapView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_mission);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(false);
        mapView.setMultiTouchControls(true);

        OnlineTileSourceBase VWorldStreet = new OnlineTileSourceBase("VWorld", 0, 22, 256, "jpeg",
                new String[0], "VWorld") {

            public String getTileURLString(final long pMapTileIndex) {
                return "http://xdworld.vworld.kr:8080/2d/Satellite/service/"+ MapTileIndex.getZoom(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + ".jpeg";
            }
        };

        mapView.setTileSource(VWorldStreet);

        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(37.6096409, 126.99769700000002);
        mapController.setCenter(startPoint);

        mapView.invalidate();
    }
}

