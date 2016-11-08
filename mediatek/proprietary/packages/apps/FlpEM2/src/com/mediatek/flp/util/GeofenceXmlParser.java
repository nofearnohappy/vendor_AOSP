package com.mediatek.flp.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.hardware.location.GeofenceHardware;
import android.hardware.location.GeofenceHardwareRequest;
import android.util.Log;

import com.mediatek.flp.em.FlpGeofence.GeofenceSession;
import com.mediatek.flp.em.FlpGeofence.GeofenceState;

public class GeofenceXmlParser {
 public final static String TAG = "FlpEM2.GeofenceXmlParser";
 String mFileName;
 String mDefaultName;
 HashMap<String, GeofenceInfo> mMap;

 public String toString() {
  StringBuilder o = new StringBuilder();
  o.append("fileName=[" + mFileName + "] ");
  o.append("defaultName=[" + mDefaultName + "]\n");
  for (GeofenceInfo info : mMap.values()) {
   o.append(info + "\n");
  }
  return o.toString();
 }

 public GeofenceXmlParser() {
  mFileName = "";
  mMap = new HashMap<String, GeofenceInfo>();
 }

 public void load(String fileName) throws Exception {
  Document doc;
  mFileName = fileName;
  doc = getXmlDocument(fileName);
  mDefaultName = doc.getDocumentElement().getAttribute(
    "default_profile_name");

  NodeList list;
  list = doc.getElementsByTagName("geofence_profile");
  for (int i = 0; i < list.getLength(); i++) {
   Node node = list.item(i);
   if (node.getNodeType() == Node.ELEMENT_NODE) {
    parseOneProfile((Element) node);
   }
  }
 }

 public void save(String srcFile, String destFile, String name,
   GeofenceSession[] input) throws Exception {
  if (name == null || input == null) {
   throw new Exception("invalid inputs");
  }
  if (isProfileExist(name)) {
   throw new Exception("profile name=[" + name + "] already exists");
  }
  GeofenceInfo info = new GeofenceInfo();
  info.mName = name;
  Document doc = getXmlDocument(srcFile);
  Element root = doc.getDocumentElement();
  Element geofenceProfile = doc.createElement("geofence_profile");
  geofenceProfile.setAttribute("name", name);
  root.appendChild(geofenceProfile);

  DefaultGeofenceFinder finder = new DefaultGeofenceFinder();
  GeofenceSession def = finder.get(input);
  info.mDefault = def;
  Element defGeofence = doc.createElement("default");
  defGeofence.setAttribute("state", "" + def.mState);
  defGeofence.setAttribute("lat", "" + def.mRequest.getLatitude());
  defGeofence.setAttribute("lng", "" + def.mRequest.getLongitude());
  defGeofence.setAttribute("radius", "" + def.mRequest.getRadius());
  defGeofence.setAttribute("last_trans",
    xmlTransEncoder(def.mRequest.getLastTransition()));
  defGeofence.setAttribute("unknown_timer",
    "" + def.mRequest.getUnknownTimer());
  defGeofence.setAttribute("monitor_trans",
    xmlTransEncoder(def.mRequest.getMonitorTransitions()));
  defGeofence.setAttribute("response",
    "" + def.mRequest.getNotificationResponsiveness());
  defGeofence.setAttribute("source",
    xmlSourceEncoder(def.mRequest.getSourceTechnologies()));
  geofenceProfile.appendChild(defGeofence);

  for (int i = 0; i < input.length; i++) {
   GeofenceSession s = input[i];
   if (def.isEqual(s)) {
    continue;
   }
   info.mList.add(s.clone());
   GeofenceHardwareRequest r = s.mRequest;
   Element geofence = doc.createElement("geofence");
   geofence.setAttribute("id", "" + s.mId);
   geofence.setAttribute("state", "" + s.mState);
   geofence.setAttribute("lat", "" + r.getLatitude());
   geofence.setAttribute("lng", "" + r.getLongitude());
   geofence.setAttribute("radius", "" + r.getRadius());
   geofence.setAttribute("last_trans",
     xmlTransEncoder(r.getLastTransition()));
   geofence.setAttribute("unknown_timer", "" + r.getUnknownTimer());
   geofence.setAttribute("monitor_trans",
     xmlTransEncoder(r.getMonitorTransitions()));
   geofence.setAttribute("response",
     "" + r.getNotificationResponsiveness());
   geofence.setAttribute("source",
     xmlSourceEncoder(r.getSourceTechnologies()));
   geofenceProfile.appendChild(geofence);
  }
  mMap.put(info.mName, info);

  TransformerFactory transformerFactory = TransformerFactory
    .newInstance();
  Transformer transformer = transformerFactory.newTransformer();
  DOMSource source = new DOMSource(doc);
  StreamResult result = new StreamResult(new File(destFile));
  transformer.transform(source, result);

 }

 public String getDefaultnName() {
  return mDefaultName;
 }

 public String[] getAllNames() {
  String[] out = new String[mMap.size()];
  int i = 0;
  for (String s : mMap.keySet()) {
   out[i] = s;
   i++;
  }
  return out;
 }

 public boolean isProfileExist(String name) {
  return mMap.containsKey(name);
 }

 public boolean updateGeofences(GeofenceSession[] input, String name) {
  GeofenceInfo info = mMap.get(name);
  if (info == null) {
   return false;
  }
  if (info.mDefault != null) {
   GeofenceSession def = info.mDefault;
   for (GeofenceSession s : input) {
    s.set(def);
   }
  }
  ArrayList<GeofenceSession> list = info.mList;
  for (GeofenceSession s : list) {
   if (s.mId >= input.length) {
    log("id=" + s.mId + " is large than input.length="
      + input.length);
    continue;
   }
   GeofenceSession needModified = input[s.mId];
   needModified.set(s);
  }
  return true;
 }

 private void parseOneProfile(Element element) {
  GeofenceInfo info = new GeofenceInfo();
  info.mName = element.getAttribute("name");
  NodeList list = element.getElementsByTagName("default");
  info.mDefault = new GeofenceSession(0);
  for (int i = 0; i < list.getLength(); i++) {
   Node node = list.item(i);
   if (node.getNodeType() == Node.ELEMENT_NODE) {
    Element e = (Element) node;
    info.mDefault.mState = xmlStateDecoder(e.getAttribute("state"));
    double latitude = xmlDoubleParser(e.getAttribute("lat"));
    double longitude = xmlDoubleParser(e.getAttribute("lng"));
    double radius = xmlDoubleParser(e.getAttribute("radius"));
    int lastTransition = xmlTransDecoder(e
      .getAttribute("last_trans"));
    int unknownTimer = xmlIntParser(e.getAttribute("unknown_timer")); // ms
    int monitorTransitions = xmlTransDecoder(e
      .getAttribute("monitor_trans"));
    int notificationResponsiveness = xmlIntParser(e
      .getAttribute("response")); // ms
    int sourceTechnologies = xmlSourceDecoder(e
      .getAttribute("source"));
    info.mDefault.createCircularGeofence(latitude, longitude,
      radius, lastTransition, unknownTimer,
      monitorTransitions, notificationResponsiveness,
      sourceTechnologies);
   }
  }

  list = element.getElementsByTagName("geofence");
  for (int i = 0; i < list.getLength(); i++) {
   Node n = list.item(i);
   if (n.getNodeType() == Node.ELEMENT_NODE) {
    GeofenceSession s = new GeofenceSession(0);
    Element e = (Element) n;
    s.mId = xmlIntParser(e.getAttribute("id"));
    s.mState = xmlStateDecoder(e.getAttribute("state"));
    double latitude = xmlDoubleParser(e.getAttribute("lat"));
    double longitude = xmlDoubleParser(e.getAttribute("lng"));
    double radius = xmlDoubleParser(e.getAttribute("radius"));
    int lastTransition = xmlTransDecoder(e
      .getAttribute("last_trans"));
    int unknownTimer = xmlIntParser(e.getAttribute("unknown_timer")); // ms
    int monitorTransitions = xmlTransDecoder(e
      .getAttribute("monitor_trans"));
    int notificationResponsiveness = xmlIntParser(e
      .getAttribute("response")); // ms
    int sourceTechnologies = xmlSourceDecoder(e
      .getAttribute("source"));
    s.createCircularGeofence(latitude, longitude, radius,
      lastTransition, unknownTimer, monitorTransitions,
      notificationResponsiveness, sourceTechnologies);
    if (info.isIdExist(s.mId)) {
     log("id=" + s.mId + " already exists");
    } else {
     info.mList.add(s);
    }
   }
  }

  mMap.put(info.mName, info);
 }

 private static Document getXmlDocument(String file) throws Exception {
  File fXmlFile = new File(file);
  DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
  DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
  Document doc = dBuilder.parse(fXmlFile);
  return doc;
 }

 private GeofenceState xmlStateDecoder(String input) {
  try {
   if (input.equals("OFF")) {
    return GeofenceState.OFF;
   }
   if (input.equals("ON")) {
    return GeofenceState.ON;
   }
   if (input.equals("PAUSE")) {
    return GeofenceState.PAUSE;
   }
  } catch (Exception e) {
   e.printStackTrace();
  }
  return GeofenceState.OFF;
 }

 private int xmlTransDecoder(String input) {
  try {
   int out = 0;
   input = input.toUpperCase();
   if (input.contains("ENTER")) {
    out |= GeofenceHardware.GEOFENCE_ENTERED;
   }
   if (input.contains("EXIT")) {
    out |= GeofenceHardware.GEOFENCE_EXITED;
   }
   if (input.contains("UNCERTAIN")) {
    out |= GeofenceHardware.GEOFENCE_UNCERTAIN;
   }
   return out;
  } catch (Exception e) {
   e.printStackTrace();
  }
  return 0;
 }

 private int xmlSourceDecoder(String input) {
  try {
   int out = 0;
   input = input.toUpperCase();
   if (input.contains("GNSS")) {
    out |= GeofenceHardware.SOURCE_TECHNOLOGY_GNSS;
   }
   if (input.contains("WIFI")) {
    out |= GeofenceHardware.SOURCE_TECHNOLOGY_WIFI;
   }
   if (input.contains("SENSORS")) {
    out |= GeofenceHardware.SOURCE_TECHNOLOGY_SENSORS;
   }
   if (input.contains("CELL")) {
    out |= GeofenceHardware.SOURCE_TECHNOLOGY_CELL;
   }
   if (input.contains("BT")) {
    out |= GeofenceHardware.SOURCE_TECHNOLOGY_BLUETOOTH;
   }
   if (out == 0) {
    out = GeofenceHardware.SOURCE_TECHNOLOGY_GNSS;
   }
   return out;
  } catch (Exception e) {
   e.printStackTrace();
  }
  return GeofenceHardware.SOURCE_TECHNOLOGY_GNSS;
 }

 private String xmlTransEncoder(int input) {
  String o = "";
  if ((input & GeofenceHardware.GEOFENCE_ENTERED) != 0) {
   o += "ENTER,";
  }
  if ((input & GeofenceHardware.GEOFENCE_EXITED) != 0) {
   o += "EXIT,";
  }
  if ((input & GeofenceHardware.GEOFENCE_UNCERTAIN) != 0) {
   o += "UNCERTAIN,";
  }
  return o;
 }

 private String xmlSourceEncoder(int input) {
  String o = "";
  if ((input & GeofenceHardware.SOURCE_TECHNOLOGY_GNSS) != 0) {
   o += "GNSS,";
  }
  if ((input & GeofenceHardware.SOURCE_TECHNOLOGY_WIFI) != 0) {
   o += "WIFI,";
  }
  if ((input & GeofenceHardware.SOURCE_TECHNOLOGY_SENSORS) != 0) {
   o += "SENSORS,";
  }
  if ((input & GeofenceHardware.SOURCE_TECHNOLOGY_CELL) != 0) {
   o += "CELL,";
  }
  if ((input & GeofenceHardware.SOURCE_TECHNOLOGY_BLUETOOTH) != 0) {
   o += "BT,";
  }
  return o;
 }

 private double xmlDoubleParser(String input) {
  try {
   return Double.valueOf(input);
  } catch (Exception e) {
   e.printStackTrace();
  }
  return 0;
 }

 private int xmlIntParser(String input) {
  try {
   return Integer.valueOf(input);
  } catch (Exception e) {
   e.printStackTrace();
  }
  return 0;
 }

 public class GeofenceInfo {
  String mName;
  GeofenceSession mDefault;
  ArrayList<GeofenceSession> mList;

  public String toString() {
   StringBuilder o = new StringBuilder();
   o.append("name=[" + mName + "]\n");
   o.append("  default " + mDefault + "\n");
   for (GeofenceSession s : mList) {
    o.append("  " + s + "\n");
   }
   return o.toString();
  }

  public GeofenceInfo() {
   mList = new ArrayList<GeofenceSession>();
  }

  public boolean isIdExist(int id) {
   for (GeofenceSession s : mList) {
    if (s.mId == id) {
     return true;
    }
   }
   return false;
  }
 }

 class DefaultGeofenceFinder {
  ArrayList<GeofenceCount> mList;

  public DefaultGeofenceFinder() {
   mList = new ArrayList<GeofenceCount>();
  }

  public GeofenceSession get(GeofenceSession[] input) {
   process(input);
   return getDefault().clone();
  }

  private GeofenceSession getDefault() {
   int index = 0;
   int maxCount = -1;
   for (int i = 0; i < mList.size(); i++) {
    GeofenceCount c = mList.get(i);
    if (c.mCount > maxCount) {
     maxCount = c.mCount;
     index = i;
    }
   }
   return mList.get(index).mSession;
  }

  private void process(GeofenceSession[] input) {
   for (GeofenceSession s : input) {
    for (GeofenceCount c : mList) {
     if (c.isSame(s)) {
      c.mCount++;
      continue;
     }
    }
    GeofenceCount newOne = new GeofenceCount(s);
    mList.add(newOne);
   }
  }
 }

 class GeofenceCount {
  int mCount;
  GeofenceSession mSession;

  public GeofenceCount(GeofenceSession session) {
   mCount = 0;
   mSession = session;
  }

  public boolean isSame(GeofenceSession session) {
   return mSession.isEqual(session);
  }
 }

 public static void log(Object msg) {
  Log.d(TAG, "" + msg);
 }

}
