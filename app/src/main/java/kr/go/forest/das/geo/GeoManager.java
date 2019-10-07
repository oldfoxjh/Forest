package kr.go.forest.das.geo;

import android.location.Location;
import android.os.Environment;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dji.common.mission.waypoint.Waypoint;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.Model.RectD;

public class GeoManager {
    private static final double WGS84_RADIUS = 6370997.0;
    private static double EarthCircumFence = 2* WGS84_RADIUS * Math.PI;

    private static final String LOG_DIRECTORY = "DroneAppService/Flight_Log";
    private static final GeoManager ourInstance = new GeoManager();

    public static GeoManager getInstance() {
        return ourInstance;
    }
    private SpatialReference merc = new SpatialReference();
    private SpatialReference wgs84 = new SpatialReference();
    private CoordinateTransformation transform;

    private GeoManager() {
        // for Elevation Info(geotiff file)
        gdal.AllRegister();

        // for Shape file
        ogr.RegisterAll();

        merc.ImportFromProj4("+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +no_defs ");
        wgs84.SetWellKnownGeogCS("WGS84");
        transform = osr.CreateCoordinateTransformation(merc, wgs84);
    }

    private boolean CheckFile(String filepath) {
        File _file = new File(filepath);
        return _file.exists();
    }

    /**
     * 표고(Elevation) 정보를 DEM 파일에서 검색하여 가져온다. 이 때 이륙지점 표고를 base(0m)로 한다.
     * @param points 표고정보를 받아올 좌표목록
     * @param base 드론 이륙지점
     * @return 표고값이 반영된 좌표 목록
     */
    public int getElevations(List<GeoPoint> points, GeoPoint base) {
        boolean _complete = true;
        int _result;

        String file_path = Environment.getExternalStorageDirectory() + File.separator + "DroneAppService/DEM"+ File.separator + "Dem.tif";
        // 해당 파일이 존재하지 않음
        if (!CheckFile(file_path)) return -1;

        // geotiff file load and get Raster
        Dataset _elevationDataSet = gdal.Open(file_path, gdalconst.GA_ReadOnly);

        // 정상적인 파일이 아님
        if (_elevationDataSet == null) {
            return -2;
        }
        Band _rasterBand = _elevationDataSet.GetRasterBand(1);

        SpatialReference _src = new SpatialReference();
        _src.SetWellKnownGeogCS("WGS84");
        String _projection = _elevationDataSet.GetProjection();
        SpatialReference _dst = new SpatialReference(_projection);

        CoordinateTransformation _ct = new CoordinateTransformation(_src, _dst);

        // 시작위치 고도 적용
        int[] base_altitude = new int[2];
        if (getAltitude(base, _elevationDataSet, _rasterBand, _ct, base_altitude) != 0) {
            _complete = false;
        }

        // 각각의 웨이포인트 고도 적용
        for (int i = 0 ; i < points.size(); i++) {
            GeoPoint point = points.get(i);

            int[] altitude = new int[2];
            int _readResult = getAltitude(point, _elevationDataSet, _rasterBand, _ct, altitude);

            if (_readResult != 0) {
                _complete = false;
            }else{
                point.setAltitude(point.getAltitude() + altitude[0] - base_altitude[0]);
                LogWrapper.i("GeoManager", String.format("고도(%d) : %f", i, (point.getAltitude() + altitude[0] - base_altitude[0])));
            }
        }

        _dst.delete();
        _ct.delete();
        _rasterBand.delete();
        _elevationDataSet.delete();

        if (!_complete) return -3;          // 일부 좌표에 문제가 있음.

        return 0;
    }

    /**
     * 표고(Elevation) 정보를 DEM 파일에서 검색하여 가져온다.
     * @param point 표고정보를 받아올 좌표
     * @param data_set
     * @param band
     * @param ct
     * @param result 표고값
     * @return
     */
    private int getAltitude(GeoPoint point, Dataset data_set, Band band, CoordinateTransformation ct, int[] result){
        double[] _geoTransformsInDoubles = data_set.GetGeoTransform();
        double _latitude = point.getLatitude();
        double _longitude = point.getLongitude();

        double[] _xy = ct.TransformPoint(_longitude, _latitude);
        int _x = (int) (((_xy[0] - _geoTransformsInDoubles[0]) / _geoTransformsInDoubles[1]));
        int _y = (int) (((_xy[1] - _geoTransformsInDoubles[3]) / _geoTransformsInDoubles[5]));

        return  band.ReadRaster(_x, _y, 1, 1, result);
    }

    /**
     * Shape 파일로 부터 좌표정보를 가져온다
     * @param filepath Shape 파일이 있는 위치
     * @param waypoints 좌표정보가 들어가 있는 목록
     * @return 성공여부
     */
    public int getPositionsFromShapeFile(String filepath, List<GeoPoint> waypoints) {
        ArrayList<GeoPoint> _return = new ArrayList<GeoPoint>();

        if (!CheckFile(filepath)) return -1;

        DataSource _ds = ogr.Open(filepath, true);

        if (_ds == null) {                      // 파일 분석 안됨
            return -2;
        }

        int _ds_count = _ds.GetLayerCount();

        if(_ds_count > 1) {
            return -3;                          // 레이어 1개 이상이면 안됨
        }

        for (int iLayer = 0; iLayer < _ds_count; iLayer++) {
            Layer _poLayer = _ds.GetLayer(iLayer);

            if (_poLayer == null) {
                return -4;                      // 정상적인 레이어 아님
            }

            long _featureCount = _poLayer.GetFeatureCount();

            for (int iFeature = 0; iFeature < _featureCount; iFeature++) {
                Feature _feature = _poLayer.GetFeature(iFeature);
                Geometry _geometry = _feature.GetGeometryRef();

                String _json = _geometry.ExportToJson();
                JSONArray _object = null;
                try {
                    _object = new JSONObject(_json).getJSONArray("coordinates");
                    for(int i = 0; i < _object.length() ; i++)
                    {
                        JSONArray _point = _object.getJSONArray(i);
                        GeoPoint _geo = new GeoPoint(_point.getDouble(0), _point.getDouble(1), _point.getDouble(2));
                        waypoints.add(_geo);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        _ds.delete();
        return 0;
    }

    /**
     *  생성된 좌표를 Shape 파일로 변환
     * @param file_name 저장할 파일명
     * @param positions 저장할 좌표 목록
     * @return 성공여부
     */
    public int saveShapeFile(String file_name, List<GeoPoint> positions) {

        // 저장할  Directory 체크 - 오늘 날짜
        Date _date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        _date.setTime(System.currentTimeMillis());
        StringBuilder ret = new StringBuilder(80);
        ret.append(formatter.format(_date));
        String _folder_path = Environment.getExternalStorageDirectory() + File.separator + LOG_DIRECTORY + File.separator + ret.toString();
        File _folder = new File(_folder_path);
        if(!_folder.exists()) {
            _folder.mkdirs();
        }

        // save file using ogr driver
        Driver drv = ogr.GetDriverByName("ESRI Shapefile");

        if (drv.TestCapability(ogr.ODrCCreateDataSource) == false) {
            //System.err.println( pszFormat + " driver does not support data source creation.");
            return -99;
        }

        String _file_path = _folder_path + File.separator + file_name;

        File file = new File(_file_path);
        if (file.exists())
            drv.DeleteDataSource(_file_path);

        DataSource outputDs = drv.CreateDataSource(_file_path);

        if (outputDs == null) {
            //System.err.println( pszFormat + " driver failed to create "+ pszDestDataSource );
            return -98;
        }

        SpatialReference _dst = new SpatialReference();
        _dst.SetWellKnownGeogCS("WGS84");
        Layer _outLayer = outputDs.CreateLayer(_file_path, _dst);

        FieldDefn field_def = new FieldDefn("DN", ogr.OFTInteger);
        _outLayer.CreateField(field_def);

        // make geometry data
        Geometry _saveInfo = new Geometry(ogr.wkbLineString);

        for (GeoPoint position : positions) {
            _saveInfo.AddPoint(position.getLatitude(), position.getLongitude(), position.getAltitude());
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

    /**
     * merc 좌표계를 WGS84 좌표계로 변환
     */
    public GeoPoint getWGS84Points(double x, double y) {
            double[] _result = transform.TransformPoint(x, y);
            GeoPoint _point = new GeoPoint(_result[1], _result[0]);

            return _point;
    }

    /**
     *  WGS84 좌표계에서 두 지점 사이의 거리
     * @param startLatitude : 시작점 위도
     * @param startLongitude : 시작점 경도
     * @param endLatitude : 끝점 위도
     * @param endLongitude : 끝점 경도
     * @return : 두 지점 사이의 거리(m)
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
     * @param points 거리를 계산할 위치들
     * @return 전체거리(m)
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
     * @param points : 사용자가 선택한 좌표
     * @param unit : 반환 단위
     * @return 다각형의 면적
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
     * @param points : 사용자가 선택한 좌표
     * @return 다각형을 포함하는 RectD 구조체 반환
     */
    public RectD getPolygonBoundRect(List<GeoPoint> points)
    {
        return new RectD(points);
    }


    /**
     * 주어진 좌표에서 동서, 남북의 거리에 위치한 좌표 구하기
     * @param source_point : 주어진 좌표
     * @param east_west : 동서방향의 거리(동: +, 서: -)
     * @param north_south : 남북방향의 거리(북 : +, 남 : -)
     * @return 주어진 좌표에서 동서, 남북의 거리에 위치한 좌표
     */
    public GeoPoint getPositionFromDistance(GeoPoint source_point, double east_west, double north_south){
        double degreesPerMeterForLat = EarthCircumFence/360.0;
        double shrinkFactor = Math.cos((source_point.getLatitude()*Math.PI/180));
        double degreesPerMeterForLon = degreesPerMeterForLat * shrinkFactor;
        double newLat = source_point.getLatitude() + north_south * (1/degreesPerMeterForLat);
        double newLng = source_point.getLongitude() + east_west * (1/degreesPerMeterForLon);
        return new GeoPoint(newLat, newLng);
    }

    private List<GeoPoint> getLines(List<GeoPoint> rect, double overlap, int degree){
        List<GeoPoint> _result = new ArrayList<>();

        double _height = distance(rect.get(0).getLatitude(), rect.get(0).getLongitude(), rect.get(3).getLatitude(), rect.get(3).getLongitude());
        double _width = distance(rect.get(0).getLatitude(), rect.get(0).getLongitude(), rect.get(1).getLatitude(), rect.get(1).getLongitude());
        int count = (_height > _width) ? (int)(_height/overlap)*2 : (int)(_width/overlap)*2;
        double _sin = Math.sin(Math.toRadians(degree));
        double _cos = Math.cos(Math.toRadians(degree));
        double ew_factor = overlap*_cos;
        double ns_factor = overlap*_sin;

        GeoPoint x1;
        GeoPoint x2;

        x1 = getPositionFromDistance(rect.get(1), -Math.max(_height, _width)*2*_sin, -Math.max(_height, _width)*2*_cos);
        x2 = getPositionFromDistance(rect.get(1), Math.max(_height, _width)*2*_sin, Math.max(_height, _width)*2*_cos);

        GeoPoint top;
        GeoPoint bottom;
        GeoPoint _intersect;

        for(int i = -count; i < count ; i++){
            top = getPositionFromDistance(x2, -ew_factor*i, ns_factor*i);
            bottom = getPositionFromDistance(x1, -ew_factor*i, ns_factor*i);
            // 경계선과 교차점 찾기
            _intersect = getIntersectPoint(top.getLongitude(), top.getLatitude(), bottom.getLongitude(), bottom.getLatitude()
                    , rect.get(1).getLongitude(), rect.get(1).getLatitude(), rect.get(3).getLongitude(), rect.get(3).getLatitude());
            if(_intersect == null) {
                _intersect = getIntersectPoint(top.getLongitude(), top.getLatitude(), bottom.getLongitude(), bottom.getLatitude()
                        , rect.get(0).getLongitude(), rect.get(0).getLatitude(), rect.get(2).getLongitude(), rect.get(2).getLatitude());
            }

            if(_intersect != null ){
                if (Math.abs(i % 2) == 1) {
                    _result.add(top);
                    _result.add(bottom);
                } else {
                    _result.add(bottom);
                    _result.add(top);
                }
            }
        }

        return _result;
    }
    /**
     * 비행경로와 촬영영역과 교차하는 지점 찾기
     * @param x1    비행경로 첫번째 좌표
     * @param x2    비행경로 두번째 좌표
     * @param waypoints 촬영영역 좌표
     * @return
     */
    private List<GeoPoint> getIntersects(GeoPoint x1, GeoPoint x2, List<GeoPoint> waypoints) {
        List<GeoPoint> _result = new ArrayList<>();
        for(int j = 0; j < waypoints.size() ; j++){
            GeoPoint x3 = waypoints.get(j);
            GeoPoint x4;
            if(j == waypoints.size() - 1){
                x4 = waypoints.get(0);
            }else x4 = waypoints.get(j+1);

            // 교차점
            GeoPoint _intersect = getIntersectPoint(x1.getLongitude(), x1.getLatitude(), x2.getLongitude(), x2.getLatitude()
                    , x3.getLongitude(), x3.getLatitude(), x4.getLongitude(), x4.getLatitude());

            // 교차지점 추가
            if(_intersect != null){
                // 비행경로 추가
                _result.add(_intersect);
            }
        }

        return _result;
    }

    /**
     * 주어진 점들의 경계면의 구획을 나누는 점들을 구하기
     * @param waypoints 사용자가 선택한 좌표
     * @param overlap 좌우 간격
     * @param degree 회전
     * @return 주어진 경계면의 구획을 나누는 점들
     */
    public List<GeoPoint> getPositionsFromRectD(List<GeoPoint> waypoints, double overlap, int degree){
        List<GeoPoint> _points = getLines(new RectD(waypoints).getPoints(), overlap, degree);

        // 비행영역과 촬영영역과 교차하는 지점 찾기
        List<GeoPoint> _intersects = new ArrayList<GeoPoint>();

        for(int i = 0; i < _points.size(); i++){
            // 비행영역을 지나는 두 점
            GeoPoint x1 = _points.get(i);
            GeoPoint x2 = _points.get(++i);

            // 촬영영역을 지나는 두 지점
            List<GeoPoint> _temp = getIntersects(x1, x2, waypoints);

            // 교차점이 없을 경우 처리
            if(_temp.size() < 1) continue;

            // 비행경로 순서
//            int _degree = degree%90;
//            double _radian = Math.toRadians(_degree);
//            double _sin = Math.sin(_radian);
//            double _cos = Math.cos(_radian);

//            if(degree < 90){
//                if(((_intersects.size()/2)%2) == 0){
//                    _intersects.add(getPositionFromDistance(getMaxLatitude(_temp), _sin*TURNAROUND_DISTANCE, _cos*TURNAROUND_DISTANCE));
//                    _intersects.add(getPositionFromDistance(getMinLatitude(_temp), -_sin*TURNAROUND_DISTANCE, -_cos*TURNAROUND_DISTANCE));
//                }else{
//                    _intersects.add(getPositionFromDistance(getMinLatitude(_temp), -_sin*TURNAROUND_DISTANCE, -_cos*TURNAROUND_DISTANCE));
//                    _intersects.add(getPositionFromDistance(getMaxLatitude(_temp), _sin*TURNAROUND_DISTANCE, _cos*TURNAROUND_DISTANCE));
//                }
//            }else if(degree > 269) {
//                if(((_intersects.size()/2)%2) == 0){
//                    _intersects.add(getPositionFromDistance(getMaxLongitude(_temp), _cos*TURNAROUND_DISTANCE, -_sin*TURNAROUND_DISTANCE));
//                    _intersects.add(getPositionFromDistance(getMinLongitude(_temp), -_cos*TURNAROUND_DISTANCE, _sin*TURNAROUND_DISTANCE));
//                }else{
//                    _intersects.add(getPositionFromDistance(getMinLongitude(_temp), -_cos*TURNAROUND_DISTANCE, _sin*TURNAROUND_DISTANCE));
//                    _intersects.add(getPositionFromDistance(getMaxLongitude(_temp), _cos*TURNAROUND_DISTANCE, -_sin*TURNAROUND_DISTANCE));
//                }
//            }else if(degree < 179){
//                if(((_intersects.size()/2)%2) == 0){
//                    _intersects.add(getPositionFromDistance(getMaxLongitude(_temp), _cos*TURNAROUND_DISTANCE, -_sin*TURNAROUND_DISTANCE));
//                    _intersects.add(getPositionFromDistance(getMinLongitude(_temp), -_cos*TURNAROUND_DISTANCE, _sin*TURNAROUND_DISTANCE));
//                }else{
//                    _intersects.add(getPositionFromDistance(getMinLongitude(_temp), -_cos*TURNAROUND_DISTANCE, _sin*TURNAROUND_DISTANCE));
//                    _intersects.add(getPositionFromDistance(getMaxLongitude(_temp), _cos*TURNAROUND_DISTANCE, -_sin*TURNAROUND_DISTANCE));
//                }
//            }else{
//                if(((_intersects.size()/2)%2) == 0){
//                    _intersects.add(getPositionFromDistance(getMaxLatitude(_temp), _sin*TURNAROUND_DISTANCE, _cos*TURNAROUND_DISTANCE));
//                    _intersects.add(getPositionFromDistance(getMinLatitude(_temp), -_sin*TURNAROUND_DISTANCE, -_cos*TURNAROUND_DISTANCE));
//                }else{
//                    _intersects.add(getPositionFromDistance(getMinLatitude(_temp), -_sin*TURNAROUND_DISTANCE, -_cos*TURNAROUND_DISTANCE));
//                    _intersects.add(getPositionFromDistance(getMaxLatitude(_temp), _sin*TURNAROUND_DISTANCE, _cos*TURNAROUND_DISTANCE));
//                }
//            }

            if(degree < 90){
                if(((_intersects.size()/2)%2) == 0){
                    _intersects.add(getPositionFromDistance(getMaxLatitude(_temp), 0, 0));
                    _intersects.add(getPositionFromDistance(getMinLatitude(_temp), 0, 0));
                }else{
                    _intersects.add(getPositionFromDistance(getMinLatitude(_temp), 0, 0));
                    _intersects.add(getPositionFromDistance(getMaxLatitude(_temp), 0, 0));
                }
            }else if(degree > 269) {
                if(((_intersects.size()/2)%2) == 0){
                    _intersects.add(getPositionFromDistance(getMaxLongitude(_temp), 0, 0));
                    _intersects.add(getPositionFromDistance(getMinLongitude(_temp), 0, 0));
                }else{
                    _intersects.add(getPositionFromDistance(getMinLongitude(_temp), 0, 0));
                    _intersects.add(getPositionFromDistance(getMaxLongitude(_temp), 0, 0));
                }
            }else if(degree < 179){
                if(((_intersects.size()/2)%2) == 0){
                    _intersects.add(getPositionFromDistance(getMaxLongitude(_temp), 0, 0));
                    _intersects.add(getPositionFromDistance(getMinLongitude(_temp), 0, 0));
                }else{
                    _intersects.add(getPositionFromDistance(getMinLongitude(_temp), 0, 0));
                    _intersects.add(getPositionFromDistance(getMaxLongitude(_temp), 0, 0));
                }
            }else{
                if(((_intersects.size()/2)%2) == 0){
                    _intersects.add(getPositionFromDistance(getMaxLatitude(_temp), 0, 0));
                    _intersects.add(getPositionFromDistance(getMinLatitude(_temp), 0, 0));
                }else{
                    _intersects.add(getPositionFromDistance(getMinLatitude(_temp), 0, 0));
                    _intersects.add(getPositionFromDistance(getMaxLatitude(_temp), 0, 0));
                }
            }


            _temp.clear();
        }

        _points.clear();

        return _intersects;
    }

    /**
     * 주어진 점들에서 위도가 가장 큰 좌표 구하기
     * @param points 좌표 목록
     * @return 위도가 가장 큰 좌표
     */
    private GeoPoint getMaxLatitude(List<GeoPoint> points){
        GeoPoint _max = null;

        for(GeoPoint point : points){
            if(_max == null) _max = point;
            if(_max.getLatitude() < point.getLatitude()){
                _max = point;
            }
        }

        return _max;
    }

    /**
     * 주어진 점들에서 위도가 가장 작은 좌표 구하기
     * @param points 좌표
     * @return 경도가 가장 큰 좌표
     */
    private GeoPoint getMinLatitude(List<GeoPoint> points){
        GeoPoint _min = null;

        for(GeoPoint point : points){
            if(_min == null) _min = point;
            if(_min.getLatitude() > point.getLatitude()){
                _min = point;
            }
        }

        return _min;
    }

    /**
     * 주어진 점들에서 경도가 가장 큰 좌표 구하기
     * @param points 좌표 목록
     * @return 위도가 가장 큰 좌표
     */
    private GeoPoint getMaxLongitude(List<GeoPoint> points){
        GeoPoint _max = null;

        for(GeoPoint point : points){
            if(_max == null) _max = point;
            if(_max.getLongitude() < point.getLongitude()){
                _max = point;
            }
        }

        return _max;
    }

    /**
     * 주어진 점들에서 경도가 가장 작은 좌표 구하기
     * @param points 좌표
     * @return 경도가 가장 큰 좌표
     */
    private GeoPoint getMinLongitude(List<GeoPoint> points){
        GeoPoint _min = null;

        for(GeoPoint point : points){
            if(_min == null) _min = point;
            if(_min.getLongitude() > point.getLongitude()){
                _min = point;
            }
        }

        return _min;
    }

    /**
     * 두 선의 교차점을 구한다.
     * @param a1_x 첫번째 선에서 지나는 첫번째 좌표의 x
     * @param a1_y 첫번째 선에서 지나는 첫번째 좌표의 y
     * @param a2_x 첫번째 선에서 지나는 두번째 좌표의 x
     * @param a2_y 첫번째 선에서 지나는 두번째 좌표의 y
     * @param b1_x 두번째 선에서 지나는 첫번째 좌표의 x
     * @param b1_y 두번째 선에서 지나는 첫번째 좌표의 y
     * @param b2_x 두번째 선에서 지나는 두번째 좌표의 x
     * @param b2_y 두번째 선에서 지나는 두번째 좌표의 y
     * @return 두 선의 교차점
     */
    public GeoPoint getIntersectPoint(double a1_x, double a1_y, double a2_x, double a2_y
                                      , double b1_x, double b1_y, double b2_x, double b2_y){

        double latitude;
        double longitude;

        double under = (b2_y-b1_y)*(a2_x-a1_x)-(b2_x-b1_x)*(a2_y-a1_y);
        if(under==0) return null;

        double _t = (b2_x-b1_x)*(a1_y-b1_y) - (b2_y-b1_y)*(a1_x-b1_x);
        double _s = (a2_x-a1_x)*(a1_y-b1_y) - (a2_y-a1_y)*(a1_x-b1_x);

        double t = _t/under;
        double s = _s/under;

        if(t<0.0 || t>1.0 || s<0.0 || s>1.0) return null;
        if(_t==0 && _s==0) return null;

        longitude = a1_x + t * (double)(a2_x-a1_x);
        latitude = a1_y + t * (double)(a2_y-a1_y);

        return new GeoPoint(latitude, longitude);
    }
}
