package com.yingda.lkj.controller.backstage;

import com.yingda.lkj.beans.entity.backstage.constructioncontrolplan.ConstructionControlPlan;
import com.yingda.lkj.beans.entity.backstage.constructioncontrolplan.ConstructionControlPlanKilometerMark;
import com.yingda.lkj.beans.entity.backstage.constructioncontrolplan.ConstructionControlPlanPoint;
import com.yingda.lkj.beans.entity.backstage.constructioncontrolplan.ConstructionDailyPlan;
import com.yingda.lkj.beans.entity.backstage.line.KilometerMark;
import com.yingda.lkj.beans.entity.backstage.line.RailwayLineSection;
import com.yingda.lkj.beans.entity.backstage.location.Location;
import com.yingda.lkj.beans.entity.backstage.opc.Opc;
import com.yingda.lkj.beans.entity.backstage.opc.OpcMark;
import com.yingda.lkj.beans.exception.CustomException;
import com.yingda.lkj.beans.system.Json;
import com.yingda.lkj.beans.system.JsonMessage;
import com.yingda.lkj.controller.BaseController;
import com.yingda.lkj.service.backstage.constructioncontrolplan.ConstructionControlPlanKilometerMarkService;
import com.yingda.lkj.service.backstage.constructioncontrolplan.ConstructionControlPlanPointService;
import com.yingda.lkj.service.backstage.constructioncontrolplan.ConstructionControlPlanService;
import com.yingda.lkj.service.backstage.constructioncontrolplan.ConstructionDailyPlanService;
import com.yingda.lkj.service.backstage.line.KilometerMarkService;
import com.yingda.lkj.service.backstage.line.RailwayLineSectionService;
import com.yingda.lkj.service.backstage.line.StationRailwayLineService;
import com.yingda.lkj.service.backstage.location.LocationService;
import com.yingda.lkj.service.backstage.opc.OpcMarkService;
import com.yingda.lkj.service.backstage.opc.OpcMarkTypeService;
import com.yingda.lkj.service.backstage.opc.OpcService;
import com.yingda.lkj.service.backstage.opc.OpcTypeService;
import com.yingda.lkj.service.base.BaseService;
import com.yingda.lkj.utils.StringUtils;
import com.yingda.lkj.utils.pojo.PojoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/backstage/opcMap")
public class OpcMapController extends BaseController {

    @Autowired
    private BaseService<ConstructionControlPlan> constructionControlPlanBaseService;
    @Autowired
    private ConstructionDailyPlanService constructionDailyPlanService;
    @Autowired
    private ConstructionControlPlanService constructionControlPlanService;
    @Autowired
    private OpcService opcService;
    @Autowired
    private KilometerMarkService kilometerMarkService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private StationRailwayLineService stationRailwayLineService;
    @Autowired
    private OpcMarkService opcMarkService;
    @Autowired
    private OpcMarkTypeService opcMarkTypeService;
    @Autowired
    private OpcTypeService opcTypeService;
    @Autowired
    private ConstructionControlPlanPointService constructionControlPlanPointService;
    @Autowired
    private RailwayLineSectionService railwayLineSectionService;
    @Autowired
    private ConstructionControlPlanKilometerMarkService constructionControlPlanKilometerMarkService;

    @Autowired
    private BaseService<Location> locationBaseService;

    /**
     * ??????????????????
     */
    @RequestMapping("/initMapByConstructionControlPlanId")
    public Json initMapByConstructionControlPlanId() throws Exception {
        String stationId = req.getParameter("stationId");
        if (StringUtils.isNotEmpty(stationId))
            return initMapByStationId();

        String opcIds = req.getParameter("opcIds");
        if (StringUtils.isNotEmpty(opcIds))
            return initMapByOpcIds();


        Map<String, Object> attributes = new HashMap<>();
        String constructionControlPlanId = req.getParameter("constructionControlPlanId");
        String constructionDailyPlanId = req.getParameter("constructionDailyPlanId");

        ConstructionDailyPlan constructionDailyPlan = null;
        if (StringUtils.isNotEmpty(constructionDailyPlanId)) {
            constructionDailyPlan = constructionDailyPlanService.getById(constructionControlPlanId);
            constructionControlPlanId = constructionDailyPlan.getConstructionControlPlanId();
        }
        ConstructionControlPlan constructionControlPlan = constructionControlPlanService.getById(constructionControlPlanId);

        // ?????????list?????????????????????
        // ??????????????????
        List<RailwayLineSection> rawRailwayLineSections = new ArrayList<>();
        // ???????????????????????????
        List<RailwayLineSection> controlPlanRailwayLineSections = new ArrayList<>();
        // ?????????
        List<KilometerMark> resultKilometerMarks = new ArrayList<>();
        // ???????????????
        List<Opc> rawOpcs = new ArrayList<>();
        // ????????????????????????
        List<Opc> constrolPlanOpcs = new ArrayList<>();
        // ???????????????
        List<OpcMark> opcMarks = new ArrayList<>();

        // ????????????????????????????????????????????????????????????????????????????????????????????????????????????
        List<ConstructionControlPlanKilometerMark> constructionControlPlanKilometerMarks =
                constructionControlPlanKilometerMarkService.getByConstructionControlPlanId(constructionControlPlan.getId());

        // ???????????????????????????
        for (ConstructionControlPlanKilometerMark constructionControlPlanKilometerMark : constructionControlPlanKilometerMarks) {
            String railwayLineId = constructionControlPlanKilometerMark.getRailwayLineId();
            // ???????????????????????????
            List<RailwayLineSection> subRailwayLineSections = railwayLineSectionService.getByStationIds(List.of(constructionControlPlanKilometerMark.getStartStationId(), constructionControlPlanKilometerMark.getEndStationId()));
            locationService.fillLocations(subRailwayLineSections, Location.RAILWAY_LINE);
            rawRailwayLineSections.addAll(subRailwayLineSections);

            // ?????????????????????????????????????????????????????????????????????
            List<RailwayLineSection> usingRailwayLineSections = filterByStartStationIdAndEndStationId(
                    constructionControlPlanKilometerMark, subRailwayLineSections
            );
            locationService.fillLocations(usingRailwayLineSections);
            controlPlanRailwayLineSections.addAll(usingRailwayLineSections);

            // ???????????????
            List<KilometerMark> kilometerMarks = kilometerMarkService.getByRailwayLineSections(usingRailwayLineSections);
            locationService.fillLocations(kilometerMarks, Location.KILOMETER_MARKS);
            // ????????????????????????????????????????????????????????????????????????????????????
            List<KilometerMark> temporaryKilometerMarks = locationService.fillRailwayLineSectionsWithLocationsByPlan(
                    usingRailwayLineSections, constructionControlPlanKilometerMark.getStartKilometer(), constructionControlPlanKilometerMark.getEndKilometer()
            );
            kilometerMarks.addAll(temporaryKilometerMarks);
            resultKilometerMarks.addAll(kilometerMarks);

            // ??????????????????????????????
            List<Opc> opcs = opcService.getLineDataByStationId(constructionControlPlanKilometerMark.getStartStationId());
            if (opcs.isEmpty()) continue;
            locationService.fillLocations(opcs, Location.OPC);
            rawOpcs.addAll(opcs);

            // ????????????????????????
            List<Opc> planOpcs = PojoUtils.copyPojoList(opcs, Opc.class);

            // ?????????????????????
            List<OpcMark> planOpcMarks = opcMarkService.getByOpcs(planOpcs);
            locationService.fillLocations(planOpcMarks);

            // ???????????????????????????????????????
            Map<String, List<OpcMark>> opcMarkMap = planOpcMarks.stream().collect(Collectors.groupingBy(OpcMark::getOpcId));
            for (Opc opc : planOpcs) {
                List<OpcMark> opcMarkByOpc = opcMarkMap.get(opc.getId());
                opcService.cutOpcLocationsByDistance(opc, opcMarkByOpc, constructionControlPlanKilometerMark.getStartKilometer(),
                        constructionControlPlanKilometerMark.getEndKilometer());
            }
            constrolPlanOpcs.addAll(planOpcs);
        }
        // ???????????????
        List<ConstructionControlPlanPoint> constructionControlPlanPoints = constructionControlPlanPointService.getByPlanId(constructionControlPlan.getId());
        for (ConstructionControlPlanPoint constructionControlPlanPoint : constructionControlPlanPoints) {
            double shortestDistance = constructionControlPlanPointService.calculationShortestDistance(constructionControlPlanPoint, rawOpcs);
            constructionControlPlanPoint.setShortestDistance(shortestDistance);
        }
        locationService.fillLocations(constructionControlPlanPoints);

        attributes.put("constructionControlPlanPoints", constructionControlPlanPoints);
        attributes.put("constructionControlPlan", constructionControlPlan);
        attributes.put("opcMarkTypes", opcMarkTypeService.getAll());
        attributes.put("opcTypes", opcTypeService.getAll());
        attributes.put("opcMarks", opcMarks);
        attributes.put("rawOpcs", rawOpcs);
        attributes.put("planOpcs", constrolPlanOpcs);
        attributes.put("rawRailwayLineSections", rawRailwayLineSections);
        attributes.put("controlPlanRailwayLineSections", controlPlanRailwayLineSections);
        attributes.put("kilometerMarks", resultKilometerMarks);
        // ?????????????????????????????????????????????????????????????????????
        if (constructionDailyPlan != null) {
            attributes.put("constructionDailyPlan", constructionDailyPlan);

            // ??????????????????????????????
            List<RailwayLineSection> dailyPlanRailwayLineSections = new ArrayList<>();
            for (RailwayLineSection controlPlanRailwayLineSection : controlPlanRailwayLineSections) {
                String railwayLineId = controlPlanRailwayLineSection.getRailwayLineId();
                if (!constructionDailyPlan.getRailwayLineId().equals(railwayLineId))
                    continue;

                List<RailwayLineSection> temporaryRailwayLineSections = PojoUtils.copyPojoList(List.of(controlPlanRailwayLineSection), RailwayLineSection.class);
                for (RailwayLineSection railwayLineSection : temporaryRailwayLineSections)
                    // ??????????????????????????????temporaryRailwayLineSections??????location???controlPlanRailwayLineSections??????location??????????????????
                    railwayLineSection.setLocations(PojoUtils.copyPojoList(railwayLineSection.getLocations(), Location.class));

                // ???????????????????????????????????????????????????????????????????????????????????????
                List<KilometerMark> temporaryKilometerMarks = locationService.fillRailwayLineSectionsWithLocationsByPlan(
                        temporaryRailwayLineSections, constructionDailyPlan.getStartKilometer(), constructionDailyPlan.getEndKilometer()
                );
                dailyPlanRailwayLineSections.addAll(temporaryRailwayLineSections);
                resultKilometerMarks.addAll(temporaryKilometerMarks);
            }
            attributes.put("dailyPlanRailwayLineSections", dailyPlanRailwayLineSections);
        }
        return new Json(JsonMessage.SUCCESS, attributes);
    }

    /**
     * ??????????????????
     */
    @RequestMapping("/initMapByStationId")
    public Json initMapByStationId() throws Exception {
        Map<String, Object> attributes = new HashMap<>();

        String stationId = req.getParameter("stationId");

        // ??????????????????
        List<RailwayLineSection> rawRailwayLineSections = railwayLineSectionService.getByStationId(stationId);
        locationService.fillLocations(rawRailwayLineSections);

        // ???????????????
        List<KilometerMark> kilometerMarks = kilometerMarkService.getByRailwayLineSections(rawRailwayLineSections);
        locationService.fillLocations(kilometerMarks);

        // ???????????????
        List<Opc> rawOpcs = opcService.getLineDataByStationId(stationId);

        if (rawOpcs == null)
            throw new CustomException(JsonMessage.SUCCESS, "???????????????????????????");
        locationService.fillLocations(rawOpcs);

        // ?????????????????????
        List<OpcMark> opcMarks = opcMarkService.getByOpcs(rawOpcs);
        locationService.fillLocations(opcMarks);


        attributes.put("opcMarkTypes", opcMarkTypeService.getAll());
        attributes.put("opcTypes", opcTypeService.getAll());
        attributes.put("opcMarks", opcMarks);
        attributes.put("rawOpcs", rawOpcs);
        attributes.put("rawRailwayLineSections", rawRailwayLineSections);
        attributes.put("kilometerMarks", kilometerMarks);

        return new Json(JsonMessage.SUCCESS, attributes);
    }

    /**
     * ??????????????????
     */
    @RequestMapping("/initMapByOpcIds")
    public Json initMapByOpcIds() throws Exception {
        Map<String, Object> attributes = new HashMap<>();

        String opcIds = req.getParameter("opcIds");
        System.out.println(opcIds);

        // ??????????????????
        List<RailwayLineSection> resultRailwayLineSections = new ArrayList<>();
        // ???????????????
        List<KilometerMark> resultKilometerMarks = new ArrayList<>();
        // ???????????????
        List<Opc> resultOpcs = new ArrayList<>();
        // ?????????????????????
        List<OpcMark> resultOpcMarks = new ArrayList<>();

        for (String opcId : opcIds.split(",")) {
            Opc opc = opcService.getById(opcId);
            String stationId = opc.getLeftStationId();

            List<RailwayLineSection> railwayLineSections = railwayLineSectionService.getByStationId(stationId);
            resultRailwayLineSections.addAll(railwayLineSections);

            List<KilometerMark> kilometerMarks = kilometerMarkService.getByRailwayLineSections(railwayLineSections);
            resultKilometerMarks.addAll(kilometerMarks);

            List<Opc> opcs = new ArrayList<>(List.of(opc));
            resultOpcs.addAll(opcs);

            List<OpcMark> opcMarks = opcMarkService.getByOpcs(opcs);
            resultOpcMarks.addAll(opcMarks);
        }

        locationService.fillLocations(resultRailwayLineSections);
        locationService.fillLocations(resultKilometerMarks);
        locationService.fillLocations(resultOpcs);
        locationService.fillLocations(resultOpcMarks);


        attributes.put("opcMarkTypes", opcMarkTypeService.getAll());
        attributes.put("opcTypes", opcTypeService.getAll());
        attributes.put("opcMarks", resultOpcMarks);
        attributes.put("rawOpcs", resultOpcs);
        attributes.put("rawRailwayLineSections", resultRailwayLineSections);
        attributes.put("kilometerMarks", resultKilometerMarks);

        return new Json(JsonMessage.SUCCESS, attributes);
    }

    private List<RailwayLineSection> filterByStartStationIdAndEndStationId(
            ConstructionControlPlanKilometerMark constructionControlPlanKilometerMark,
            List<RailwayLineSection> rawRailwayLineSections
    ) throws ReflectiveOperationException {
        // ?????????????????????(downRiver)?????????????????????
        byte downriver = constructionControlPlanKilometerMark.getDownriver();
        List<RailwayLineSection> usingRailwayLineSection = new ArrayList<>();
        for (RailwayLineSection subRailwayLineSection : rawRailwayLineSections) {
            // ?????????????????????????????????????????????
            if (downriver == ConstructionControlPlanKilometerMark.UP_AND_DOWN || downriver == ConstructionControlPlanKilometerMark.SINGLE_LINE)
                usingRailwayLineSection.add(subRailwayLineSection);
            // ???????????????????????????
            if (subRailwayLineSection.getDownriver() == RailwayLineSection.SINGLE_LINE)
                usingRailwayLineSection.add(subRailwayLineSection);
            // ???????????????????????????????????????????????????
            if (subRailwayLineSection.getDownriver() == subRailwayLineSection.getDownriver())
                usingRailwayLineSection.add(subRailwayLineSection);
        }
        usingRailwayLineSection = PojoUtils.copyPojoList(usingRailwayLineSection, RailwayLineSection.class);
        for (RailwayLineSection railwayLineSection : usingRailwayLineSection)
            // ??????????????????????????????controlPlanRailwayLineSections??????location???rawRailwayLineSections??????location??????????????????
            railwayLineSection.setLocations(PojoUtils.copyPojoList(railwayLineSection.getLocations(), Location.class));

        return usingRailwayLineSection;
    }

}
