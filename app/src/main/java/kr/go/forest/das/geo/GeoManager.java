package kr.go.forest.das.geo;

import android.graphics.RectF;
import android.location.Location;
import android.widget.Toast;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Driver;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.RectL;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kr.go.forest.das.Model.RectD;

public class GeoManager {
    private static final GeoManager ourInstance = new GeoManager();

    public static GeoManager getInstance() {
        return ourInstance;
    }
    private SpatialReference merc = new SpatialReference();
    private SpatialReference wgs84 = new SpatialReference();
    private CoordinateTransformation trsform;

    private GeoManager() {
        // for Elevation Info(geotiff file)
        gdal.AllRegister();

        // for Shape file
        ogr.RegisterAll();

        merc.ImportFromProj4("+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +no_defs ");
        wgs84.SetWellKnownGeogCS("WGS84");
        trsform = osr.CreateCoordinateTransformation(merc, wgs84);
    }

    private boolean CheckFile(String filepath) {
        File _file = new File(filepath);
        return _file.exists();
    }

    // Get Elevation Info
    // filepath : geotiff file path
    // positions : positions for elevation
    // return : Elevations (m)
    public int[] GetElevations(String filepath, ArrayList<Position> positions) {
        boolean _complete = true;
        int[] _return;

        if (!CheckFile(filepath)) return null;

        // geotiff file load and get Raster
        Dataset _elevationDataSet = gdal.Open(filepath, gdalconst.GA_ReadOnly);

        if (_elevationDataSet == null) {
            return null;
        }

        double[] _geoTransformsInDoubles = _elevationDataSet.GetGeoTransform();
        Band _rasterBand = _elevationDataSet.GetRasterBand(1);

        SpatialReference _src = new SpatialReference();
        _src.SetWellKnownGeogCS("WGS84");
        String _projection = _elevationDataSet.GetProjection();
        SpatialReference _dst = new SpatialReference(_projection);

        CoordinateTransformation _ct = new CoordinateTransformation(_src, _dst);

        int _count = positions.size();
        _return = new int[_count];

        for (int i = 0; i < _count; i++) {
            Position _position = positions.get(i);
            double _latitude = _position.latitude;
            double _longitude = _position.longitude;
            double[] _xy = _ct.TransformPoint(_longitude, _latitude);
            int _x = (int) (((_xy[0] - _geoTransformsInDoubles[0]) / _geoTransformsInDoubles[1]));
            int _y = (int) (((_xy[1] - _geoTransformsInDoubles[3]) / _geoTransformsInDoubles[5]));

            int[] flt = new int[2];
            int _readResult = _rasterBand.ReadRaster(_x, _y, 1, 1, flt);

            if (_readResult != 0) {
                _complete = false;
                break;
            }
        }

        _dst.delete();
        _ct.delete();
        _rasterBand.delete();
        _elevationDataSet.delete();

        if (!_complete) return null;

        return _return;
    }

    public ArrayList<Position> getPositionsFromShapeFile(String filepath) {
        ArrayList<Position> _return = new ArrayList<Position>();

        if (!CheckFile(filepath)) return null;

        DataSource _ds = ogr.Open(filepath, true);

        if (_ds == null) {
            return null;
        }

        int _ds_count = _ds.GetLayerCount();

        for (int iLayer = 0; iLayer < _ds_count; iLayer++) {
            Layer _poLayer = _ds.GetLayer(iLayer);

            if (_poLayer == null) {
                return null;
            }

            long _featureCount = _poLayer.GetFeatureCount();

            for (int iFeature = 0; iFeature < _featureCount; iFeature++) {
                Feature _feature = _poLayer.GetFeature(iFeature);
                Geometry _geometry = _feature.GetGeometryRef();
                int srcType = _geometry.GetGeometryType();

                if (srcType == ogr.wkbPolygon) {
                    String _json = _geometry.ExportToJson();
                } else if (srcType == ogr.wkbMultiPoint) {

                }
            }
        }

        _ds.delete();
        return _return;
    }

    /**
     * 생성된 좌표를 Shape 파일로 변환
     */
    public int saveShapeFile(String filepath, List<GeoPoint> positions) {
        // save file using ogr driver
        Driver drv = ogr.GetDriverByName("ESRI Shapefile");

        if (drv.TestCapability(ogr.ODrCCreateDataSource) == false) {
            //System.err.println( pszFormat + " driver does not support data source creation.");
            return -99;
        }

        File file = new File(filepath);
        if (file.exists())
            drv.DeleteDataSource(filepath);

        DataSource outputDs = drv.CreateDataSource(filepath);

        if (outputDs == null) {
            //System.err.println( pszFormat + " driver failed to create "+ pszDestDataSource );
            return -98;
        }

        SpatialReference _dst = new SpatialReference();
        _dst.SetWellKnownGeogCS("WGS84");
        Layer _outLayer = outputDs.CreateLayer(filepath, _dst);

        FieldDefn field_def = new FieldDefn("DN", ogr.OFTInteger);
        _outLayer.CreateField(field_def);

        // make geometry data
        Geometry _saveInfo = new Geometry(ogr.wkbLineString);

        for (GeoPoint position : positions) {
           // Geometry _point = new Geometry(ogr.wkbPoint);
           // _point.AddPoint_2D(, );
            _saveInfo.AddPoint(position.getLatitude(), position.getLongitude());
           // _point.delete();
        }

        String wkt = _saveInfo.ExportToJson();

        FeatureDefn _featureDefn = _outLayer.GetLayerDefn();
        Feature _feature = new Feature(_featureDefn);
        _feature.SetGeometry(_saveInfo);
        int res = _outLayer.CreateFeature(_feature);

        _saveInfo.delete();
        _feature.delete();
        _outLayer.delete();
        outputDs.delete();

        if (res != 0) {
            return res;
        }

        return 0;
    }

    public String makeWKT(ArrayList<GeoPoint> points)
    {
        return  null;
    }

    /**
     * merc 좌표계를 WGS84 좌표계로 변환
     */
    public GeoPoint getWGS84Points(double x, double y) {
            double[] _result = trsform.TransformPoint(x, y);
            GeoPoint _point = new GeoPoint(_result[1], _result[0]);

            return _point;
    }

    /**
     * WGS84 좌표계에서 두 지점 사이의 거리
     */
    public double distance(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        float[] _distance = new float[2];
        Arrays.fill(_distance, 0.0F);
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, _distance);

        if (_distance[0] <= 0.0F || _distance[0] > 100000.0F) {
            _distance[0] = 0.0F;
        }

        return _distance[0];
    }

    /**
     * ,주어진 점들의 거리 계산
     */
    public int getDistanceFromPoints(List<GeoPoint> points) {

        double _distance = 0.0f;

        for(int i =0 ; i< points.size() - 1; i++)
        {
            GeoPoint _point = points.get(i);
            GeoPoint _point_next = points.get(i+1);
            _distance += distance(_point.getLatitude(), _point.getLongitude(), _point_next.getLatitude(), _point_next.getLongitude());
        }
        return  (int)(_distance);
    }

    /**
     * WGS84 좌표계에서 3개 이상의 점의 면적 계산
     */
    public double getAreaFromPoints(List<GeoPoint> points, String unit) {

        Geometry ring = new Geometry(ogr.wkbLinearRing);
        ring.AssignSpatialReference(wgs84);

        for(int i =0 ; i< points.size(); i++)
        {
            GeoPoint point = points.get(i);
            ring.AddPoint(point.getLongitude(), point.getLatitude());
        }

        Geometry poly = new Geometry(ogr.wkbPolygon);
        poly.AddGeometry(ring);

        double _area = poly.GetArea();
        if(unit.equals("ha")) _area *= 1000000;

        return  _area;
    }

    /**
     * WGS84 좌표계에서 Polygon을 포함하는 경계
     */
    public RectD getPolygonBoundRect(List<GeoPoint> points)
    {
        return new RectD(points);
    }
}
