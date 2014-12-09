package eu.trentorise.smartcampus.mobility.processor.converter;

import it.sayservice.platform.smartplanner.data.message.RoadElement;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoadType;
import it.sayservice.platform.smartplanner.data.message.alerts.CreatorType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import smartcampus.service.trentomale.data.message.Trentomale.Train;

import com.google.protobuf.ByteString;

import eu.trentorise.smartcampus.service.oraritreni.data.message.Oraritreni;
import eu.trentorise.smartcampus.service.oraritreni.data.message.Oraritreni.PartArr;
import eu.trentorise.smartcampus.service.oraritreni.data.message.Oraritreni.PartenzeArrivi;
import eu.trentorise.smartcampus.service.parcheggi.data.message.Parcheggi.Parcheggio;
import eu.trentorise.smartcampus.service.tobike.data.message.Tobike.Stazione;
import eu.trentorise.smartcampus.services.ordinanzerovereto.data.message.Ordinanzerovereto.Ordinanza;
import eu.trentorise.smartcampus.services.ordinanzerovereto.data.message.Ordinanzerovereto.Via;

public class ServiceDataConverter {

	private static final String TNBDG = "6";
	private static final String BZVR = "5";
	private static final String PARTBV = "BRENNERO,BOLZANO".toLowerCase();
	private static final String ARRBV = "ROVERETO,ALA,VERONA PORTA NUOVA, BOLOGNA C.LE,ROMA TERMINI".toLowerCase();
	private static final String PARTTB = "TRENTO".toLowerCase();
	private static final String ARRTB = "BORGO VALSUGANA EST,BASSANO DEL GRAPPA,PADOVA,VENEZIA SANTA LUCIA".toLowerCase();	
	private static final String TN_BDG = "TB_R2_G";
	private static final String BDG_TN = "TB_R2_R";
	private static final String BZ_VR = "BV_R1_G";
	private static final String VR_BZ = "BV_R1_R";			
	
	private static final String DIVIETO_DI_TRANSITO_E_DI_SOSTA = "divieto di transito e di sosta";
	private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	private static final String DIVIETO_DI_TRANSITO = "divieto di transito";
	private static final String DIVIETO_DI_SOSTA = "divieto di sosta";
	private static final String DIVIETO_DI_SOSTA_CON = "divieto di sosta con rimozione coatta";	
	
	public ServiceDataConverter() {
	}

	public List<Parking> convertParcheggi(List<ByteString> data, String source) {
		List<Parking> list = new ArrayList<Parking>();
		for (ByteString bs : data) {
			try {
				Parcheggio t = Parcheggio.parseFrom(bs);
				Parking p = new Parking();
				p.setAgencyId("COMUNE_DI_ROVERETO");
				p.setId(t.getId());
				p.setAddress(t.getAddress());
				p.setFreePlaces(Integer.parseInt(t.getPlaces()));
				p.setVehicles(-1);
				list.add(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	public List<Parking> convertBikeParcheggi(List<ByteString> data) {
		List<Parking> list = new ArrayList<Parking>();
		for (ByteString bs : data) {
			try {
				Stazione s = Stazione.parseFrom(bs);
				Parking p = new Parking();
				p.setAgencyId("BIKE_SHARING_TOBIKE_ROVERETO");
				p.setId(s.getNome() + " - Rovereto");
				p.setAddress(s.getIndirizzo());
				p.setFreePlaces(s.getPosti());
				p.setVehicles(s.getBiciclette());
				list.add(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return list;
	}	
	
	public List<GenericTrain> convertTreni(List<ByteString> data) {
		List<GenericTrain> list = new ArrayList<GenericTrain>();
		for (ByteString bs : data) {
			try {
				PartenzeArrivi pa = PartenzeArrivi.parseFrom(bs);
				list = buildTrain(pa);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}	
	
	public List<GenericTrain> convertTrentoMale(List<ByteString> data) {
		List<GenericTrain> list = new ArrayList<GenericTrain>();
		for (ByteString bs : data) {
			try {
				Train t = Train.parseFrom(bs);
				GenericTrain tmt = new GenericTrain();
					tmt.setDelay(t.getDelay());
					tmt.setId("" + t.getId());
					tmt.setTripId("" + t.getNumber());
					tmt.setDirection(t.getDirection());
					tmt.setTime(t.getTime());
					tmt.setStation(t.getStation());
					tmt.setAgencyId("10");
					if ("Trento".equalsIgnoreCase(t.getDirection())) {
						tmt.setRouteId("556");
					} else {
						tmt.setRouteId("555");
					}
					list.add(tmt);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return list;
	}		
	
	public List<AlertRoad> convertOrdinanze(List<ByteString> data) {
		List<AlertRoad> list = new ArrayList<AlertRoad>();
		for (ByteString bs : data) {
			try {
				Ordinanza t = Ordinanza.parseFrom(bs);
				if (t.getVieCount() == 0) continue;
				for (int i = 0; i < t.getVieCount(); i++) {
					Via via = t.getVie(i);
					AlertRoad ar = new AlertRoad();
					ar.setAgencyId("COMUNE_DI_ROVERETO");
					ar.setCreatorType(CreatorType.SERVICE);
					ar.setDescription(t.getOgetto());
					ar.setEffect(via.hasTipologia() && !via.getTipologia().isEmpty() ? via.getTipologia() : t.getTipologia());
					ar.setFrom(sdf.parse(t.getDal()).getTime());
					ar.setTo(sdf.parse(t.getAl()).getTime());
					ar.setId(t.getId()+"_"+via.getCodiceVia());
					ar.setRoad(toRoadElement(via,t));
					ar.setChangeTypes(getTypes(via,t));
//					if (!t.getTipologia().equals("Permanente") || ar.getFrom() > c.getTimeInMillis()) 
//					{
						list.add(ar);
//					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}		
	
	
	private List<GenericTrain> buildTrain(PartenzeArrivi pa) {
		List<GenericTrain> result = new ArrayList<GenericTrain>();

		Map<String, PartArr> partMap = new TreeMap<String, Oraritreni.PartArr>();
		Map<String, PartArr> arrMap = new TreeMap<String, Oraritreni.PartArr>();
		Map<String, List<String>> agencyMap = new TreeMap<String, List<String>>();
		Map<String, List<String>> routeMap = new TreeMap<String, List<String>>();
		Map<String, List<String>> directionMap = new TreeMap<String, List<String>>();

		for (PartArr part : pa.getPart().getPartenzaList()) {
			partMap.put(part.getCodtreno(), part);
			agencyMap.put(part.getCodtreno(), new ArrayList<String>());
			routeMap.put(part.getCodtreno(), new ArrayList<String>());
			directionMap.put(part.getCodtreno(), new ArrayList<String>());
		}
		for (PartArr arr : pa.getArr().getArrivoList()) {
			arrMap.put(arr.getCodtreno(), arr);
			agencyMap.put(arr.getCodtreno(), new ArrayList<String>());
			routeMap.put(arr.getCodtreno(), new ArrayList<String>());
			directionMap.put(arr.getCodtreno(), new ArrayList<String>());
		}

		for (String akey : arrMap.keySet()) {
			PartArr arr = arrMap.get(akey);
			String cod = arr.getCodtreno();
			if ("trento".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(routeMap, cod, VR_BZ);
					addToMap(directionMap, cod, "TRENTO*");
				}
				if (PARTBV.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(routeMap, cod, BZ_VR);
					addToMap(directionMap, cod, "TRENTO*");
				}
				if (ARRTB.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, BDG_TN);
					addToMap(directionMap, cod, "TRENTO*");
				}
				if (PARTTB.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, "TRENTO*");
				}
			} else if ("bassano del grappa".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(agencyMap, cod, TNBDG);
					addToMap(directionMap, cod, "TRENTO*");
					addToMap(routeMap, cod, VR_BZ);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, "BASSANO DEL GRAPPA*");
				}
				if (PARTBV.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(agencyMap, cod, TNBDG);
					addToMap(directionMap, cod, "TRENTO*");
					addToMap(routeMap, cod, BZ_VR);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, "BASSANO DEL GRAPPA*");
				}
				if (ARRTB.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, BDG_TN);
					addToMap(directionMap, cod, "BASSANO DEL GRAPPA*");
				}
				if (PARTTB.contains(arr.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, "BASSANO DEL GRAPPA*");
				}
			}
		}

		for (String pkey : partMap.keySet()) {
			PartArr part = partMap.get(pkey);
			String cod = part.getCodtreno();
			if ("trento".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(routeMap, cod, BZ_VR);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (PARTBV.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(routeMap, cod, VR_BZ);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (ARRTB.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (PARTTB.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, BDG_TN);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
			} else if ("bassano del grappa".equalsIgnoreCase(pa.getStazione())) {
				if (ARRBV.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(agencyMap, cod, TNBDG);
					addToMap(directionMap, cod, part.getFromOrTo());
					addToMap(routeMap, cod, BZ_VR);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (PARTBV.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, BZVR);
					addToMap(agencyMap, cod, TNBDG);
					addToMap(directionMap, cod, part.getFromOrTo());
					addToMap(routeMap, cod, VR_BZ);
					addToMap(routeMap, cod, TN_BDG);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (ARRTB.contains(part.getFromOrTo().toLowerCase())) {
//					addToMap(agencyMap, cod, TNBDG);
//					addToMap(routeMap, cod, TN_BDG);
//					addToMap(directionMap, cod, part.getFromOrTo());
				}
				if (PARTTB.contains(part.getFromOrTo().toLowerCase())) {
					addToMap(agencyMap, cod, TNBDG);
					addToMap(routeMap, cod, BDG_TN);
					addToMap(directionMap, cod, part.getFromOrTo());
				}
			}

		}

		for (String key : agencyMap.keySet()) {
			String from = null;
			String to = null;
			if (partMap.containsKey(key)) {
				to = partMap.get(key).getFromOrTo();
			}
			if (arrMap.containsKey(key)) {
				from = arrMap.get(key).getFromOrTo();
			}
			List<String> ags = agencyMap.get(key);
			List<String> rts = routeMap.get(key);
			List<String> drs = directionMap.get(key);
			PartArr train = null;
			if (partMap.containsKey(key)) {
				train = partMap.get(key);
			} else if (arrMap.containsKey(key)) {
				train = arrMap.get(key);
			}
			
			String direction = to;
			if (direction == null) {
				direction = pa.getStazione().toUpperCase();
			}
			
			if (train != null) {
			List<GenericTrain> trains = buildTrains(train, pa.getStazione(), ags, rts, direction);
			result.addAll(trains);
			} 
		}

		return result;
	}

	private void addToMap(Map<String, List<String>> map, String key, String element) {
		if (!map.get(key).contains(element)) {
			map.get(key).add(element);
		}
	}

	private List<GenericTrain> buildTrains(PartArr arr, String station, List<String> agencyIds, List<String> routeIds, String direction) {
		List<GenericTrain> result = new ArrayList<GenericTrain>();

		for (int i = 0; i < agencyIds.size(); i++) {
			GenericTrain gt = new GenericTrain();
			long delay = Long.parseLong("0" + arr.getRitardo().replaceAll("\\D", ""));
			gt.setDelay(delay);
			gt.setDirection(direction);
			gt.setStation(station);
			gt.setTime(arr.getOra());
			gt.setId(arr.getCodtreno());
			gt.setAgencyId(agencyIds.get(i));
			gt.setRouteId(routeIds.get(i));
			gt.setTripId(buildTripId(arr.getCodtreno(), (BZVR.equals(agencyIds.get(i)) ? true : false)));
			result.add(gt);
		}

		return result;
	}

	private String buildTripId(String codTreno, boolean byLength) {
		String res = codTreno.replaceAll(" ", "");
		res = res.replaceAll("REG", "R");
		if (byLength && res.length() == 5) {
			res = res.replaceAll("R", "RV");
		}
		res = res.replaceAll("ES\\*", "ESAV");
		return res;
	}	
	
	private AlertRoadType[] getTypes(Via via, Ordinanza t) {
		String type = via.hasTipologia() && ! via.getTipologia().isEmpty() ? via.getTipologia() : t.getTipologia();
		if (DIVIETO_DI_TRANSITO_E_DI_SOSTA.equals(type) || t.getOgetto().toLowerCase().contains(DIVIETO_DI_TRANSITO_E_DI_SOSTA)) {
			return new AlertRoadType[]{AlertRoadType.PARKING_BLOCK, AlertRoadType.ROAD_BLOCK};
		}
		if (DIVIETO_DI_TRANSITO.equals(type) || t.getOgetto().toLowerCase().contains(DIVIETO_DI_TRANSITO)) {
			return new AlertRoadType[]{AlertRoadType.ROAD_BLOCK};
		}
		if (DIVIETO_DI_SOSTA.equals(type) || DIVIETO_DI_SOSTA_CON.equals(type) || t.getOgetto().toLowerCase().contains(DIVIETO_DI_SOSTA)) {
			return new AlertRoadType[]{AlertRoadType.PARKING_BLOCK};
		}
		if ("senso unico alternato".equals(type) || t.getOgetto().toLowerCase().contains("senso unico alternato")) {
			return new AlertRoadType[]{AlertRoadType.DRIVE_CHANGE};
		}
		if ("doppio senso di marcia".equals(type) || t.getOgetto().toLowerCase().contains("doppio senso di marcia")) {
			return new AlertRoadType[]{AlertRoadType.DRIVE_CHANGE};
		}
		if (type.contains("limitazione della velocit")) {
			return new AlertRoadType[]{AlertRoadType.DRIVE_CHANGE};
		}
		return new AlertRoadType[]{AlertRoadType.OTHER};
	}

	private RoadElement toRoadElement(Via via, Ordinanza t) {
		RoadElement re = new RoadElement();
		re.setLat(via.getLat()+"");
		re.setLon(via.getLng()+"");
		if (via.hasAlCivico()) re.setToNumber(via.getAlCivico());
		if (via.hasAlIntersezione()) re.setToIntersection(via.getAlIntersezione());
		if (via.hasCodiceVia()) re.setStreetCode(via.getCodiceVia());
		if (via.hasDalCivico()) re.setFromNumber(via.getDalCivico());
		if (via.hasDalIntersezione()) re.setFromIntersection(via.getDalIntersezione());
		if (via.hasDescrizioneVia()) re.setStreet(via.getDescrizioneVia());
		if (via.hasNote()) re.setNote(via.getNote());
		return re;
	}	
		
	
}