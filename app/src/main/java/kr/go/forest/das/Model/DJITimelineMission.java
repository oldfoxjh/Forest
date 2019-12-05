package kr.go.forest.das.Model;

import android.util.Log;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import dji.common.gimbal.Attitude;
import dji.common.gimbal.Rotation;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.model.LocationCoordinate2D;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.TimelineMission;
import dji.sdk.mission.timeline.actions.GimbalAttitudeAction;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.mission.timeline.actions.ShootPhotoAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import kr.go.forest.das.DroneApplication;

public class DJITimelineMission {

    private MissionControl missionControl;

    public DJITimelineMission(List<WaypointMission> waypoints, GeoPoint base_point){
        List<TimelineElement> elements = new ArrayList<>();
        missionControl = MissionControl.getInstance();

        final TimelineEvent preEvent = null;

        //이륙
        elements.add(new TakeOffAction());

        // 짐벌 각도 조절
        Attitude attitude = new Attitude(-90, Rotation.NO_ROTATION, Rotation.NO_ROTATION);
        GimbalAttitudeAction gimbalAction = new GimbalAttitudeAction(attitude);
        gimbalAction.setCompletionTime(3);
        elements.add(gimbalAction);

        //임무비행 시작
        for(WaypointMission mission : waypoints){
            TimelineElement waypointMission = TimelineMission.elementFromWaypointMission(mission);
            elements.add(waypointMission);
        }

        //짐벌 원위치
        attitude = new Attitude(0, Rotation.NO_ROTATION, Rotation.NO_ROTATION);
        gimbalAction = new GimbalAttitudeAction(attitude);
        gimbalAction.setCompletionTime(3);
        elements.add(gimbalAction);

        //자동복귀
        elements.add(new GoHomeAction());

        if (missionControl.scheduledCount() > 0) {
            missionControl.unscheduleEverything();
            missionControl.removeAllListeners();
        }

        missionControl.scheduleElements(elements);
    }

    public DJITimelineMission(List<WaypointMission> waypoints, GeoPoint base_point, int count, int interval, float flight_speed){
        List<TimelineElement> elements = new ArrayList<>();
        missionControl = MissionControl.getInstance();

        final TimelineEvent preEvent = null;

        //이륙
        elements.add(new TakeOffAction());

        // 짐벌 각도 조절
        Attitude attitude = new Attitude(-90, Rotation.NO_ROTATION, Rotation.NO_ROTATION);
        GimbalAttitudeAction gimbalAction = new GimbalAttitudeAction(attitude);
        gimbalAction.setCompletionTime(3);
        elements.add(gimbalAction);

        // 상승 미션
        elements.add(TimelineMission.elementFromWaypointMission(new DJIWaypointMission().getDJIMission(base_point, waypoints.get(0).getWaypointList().get(0), flight_speed)));

        // 카메라 촬영 시작
        elements.add(ShootPhotoAction.newShootIntervalPhotoAction(count,interval));

        //임무비행 시작
        for(WaypointMission mission : waypoints){
            TimelineElement waypointMission = TimelineMission.elementFromWaypointMission(mission);
            elements.add(waypointMission);
        }

        elements.add(ShootPhotoAction.newStopIntervalPhotoAction());

        //짐벌 원위치
        attitude = new Attitude(0, Rotation.NO_ROTATION, Rotation.NO_ROTATION);
        gimbalAction = new GimbalAttitudeAction(attitude);
        gimbalAction.setCompletionTime(3);
        elements.add(gimbalAction);

        //자동복귀
        elements.add(new GoHomeAction());

        if (missionControl.scheduledCount() > 0) {
            missionControl.unscheduleEverything();
            missionControl.removeAllListeners();
        }

        missionControl.scheduleElements(elements);
    }
}
