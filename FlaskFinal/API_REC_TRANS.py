'''Nam Pham, Paul Schroeder'''

import requests,sqlite3,time,datetime,json, objectpath, traceback, atexit
from threading import Thread
from datetime import datetime,timedelta
from dateutil.parser import parse
import asyncio

'''Datenbank fuer Taxifahrer'''
DatabaseFile = 't4.sqlite3'

'''ID und Code für Developer.here '''
mapID = "FCNS06QeItEtJinJGrbA"
mapCode = "Bf4Os0ScXYrDgIkNnvw1DA"

'''Zugriffsdaten fuer die Hue Lampen'''
#ip = "localhost:80" 
#user = "newdeveloper"

ip = "10.28.209.13:9001"
user = "3dc1d8f23e55321f3c049c03ac88dff"

'''wandelt die angegebenen Orte in Geokoordinaten um. Diese werden als Angabe für die Methode RestData benoetigt. Die Geokoordinaten werden aus dem von der Rest-Schnittstelle zurueck gegebenen Json statisch ausgelesen'''
def changePlaceToGeoC(place):
    requestReturn = requests.get("https://geocoder.api.here.com/6.2/geocode.json?app_id={0}&app_code={1}&searchtext={2}".format(mapID, mapCode, place)).json()
    result = json.dumps(requestReturn)
    JsonString = json.loads(result)
    return [JsonString['Response']['View'][0]['Result'][0]['Location']['DisplayPosition']['Latitude'], JsonString['Response']['View'][0]['Result'][0]['Location']['DisplayPosition']['Longitude']]

'''holt von der Rest-Schnittstelle die Dauer der Fahrt und berechnet die Abfahrtszeit (gewuenschte Ankunftszeit - Dauer)'''
def mapRestDauerAbfahrt(start, stop, time):
    startpoint = changePlaceToGeoC(start)
    stoppoint = changePlaceToGeoC(stop)
    requestReturn = requests.get("https://route.api.here.com/routing/7.2/calculateroute.json?app_id={0}&app_code={1}&waypoint0=geo!{2},{3}&waypoint1=geo!{4},{5}&mode=fastest;car;traffic:enabled".format(mapID, mapCode, startpoint[0], startpoint[1], stoppoint[0], stoppoint[1])).json()
    result = json.dumps(requestReturn)
    JsonString = json.loads(result)
    json_tree = objectpath.Tree(JsonString['response'])
    result_tuple = tuple(json_tree.execute('$..travelTime'))
    dauer = max(result_tuple)
    save = extractTime(time)
    date = datetime(save[0], save[1], save[2], save[3], save[4], 00)
    return [dauer,date - timedelta(seconds=dauer)]

'''holt von der Rest-Schnittstelle die Dauer der Fahrt. Wird bei einem Update der Fahrt benoetigt'''
def mapRestNeueAnkunft(start, stop):
    startpoint = changePlaceToGeoC(start)
    stoppoint = changePlaceToGeoC(stop)
    requestReturn = requests.get("https://route.api.here.com/routing/7.2/calculateroute.json?app_id={0}&app_code={1}&waypoint0=geo!{2},{3}&waypoint1=geo!{4},{5}&mode=fastest;car;traffic:enabled".format(mapID, mapCode, startpoint[0], startpoint[1], stoppoint[0], stoppoint[1])).json()
    result = json.dumps(requestReturn)
    JsonString = json.loads(result)
    json_tree = objectpath.Tree(JsonString['response'])
    result_tuple = tuple(json_tree.execute('$..travelTime'))
    dauer = max(result_tuple)
    return extractTimeReverse(str(datetime.now() +  timedelta(seconds=dauer)))

'''erzeugt ein neues datetime Objekt'''
def extractTime(time):
    return (int(time[0:4]), int(time[5:7]), int(time[8:10]), int(time[11:13]), int(time[14:16]))
    
def extractTimeReverse(time):
    return str(int(time[0:4])) + "-" + str(int(time[5:7])) + "-" + str(int(time[8:10])) + "T" + str(int(time[11:13])) + ":" + str(int(time[14:16]))

'''Prueft ob die Rest-Schnittstell die uebergebene Postition kennt'''
def isPositionValid(place):
    requestReturn = requests.get("https://geocoder.api.here.com/6.2/geocode.json?app_id={0}&app_code={1}&searchtext={2}".format(mapID, mapCode, place)).json()
    JsonString = json.dumps(requestReturn)
    return "Result" in JsonString or "result" in JsonString

'''Methode die neue Daten in die Datenbank schreibt, wenn ein Fahrer neue Daten ueber einen Curl Befehl submitted. Prueft vorher ob die Angabe gueltig ist oder dem aktuellen Zustand wiederspricht'''
def readFromApi(result):
    try :
        jsn = json.loads(result)
        driver = jsn['Driver']
        status = jsn['Status']
        place = jsn['currentPlace']
        con = sqlite3.connect(DatabaseFile)
        curs = con.cursor()
        curs.execute("SELECT ID, Status, Zielpunkt FROM stuff")
        data = curs.fetchall()
        for row in data:
            if (int(driver) == int(row[0])):
                if sta(status) == True:
                    if (isPositionValid(str(place)) == True):
                        if row[1] == "driving" or row[1] == "drivingNotInTime":
                            if status == "waiting" or status == "inaktiv":
                                newStatus(driver, status, place)
                            else:
                                try:
                                    updatePositionNeueAnkunft(place, row[2], driver)
                                except Exception as e:
                                    print(e)
                                    return wrongJsonPlace()
                            return okJson()
                        elif row[1] != "driving" and row[1] != "drivingNotInTime":
                            if status != "driving" and status != "drivingNotInTime":
                                newStatus(driver, status, place)
                                return okJson()
                            else:
                                return wrongJsonNoChange()
                    else:
                        return wrongJsonPlace()
                else:
                    return wrongJsonStatus()
        return wrongJsonDriver() 
    except Exception as e:
        return wrongJson(e)


def sta(status):
    return status == "driving" or status == "drivingNotInTime" or status == "waiting" or status == "inaktiv"

def updatePositionNeueAnkunft(start, stop, driver):
    if (start != stop):
        con = sqlite3.connect(DatabaseFile)
        curs = con.cursor()
        curs.execute("SELECT zielZeit FROM stuff WHERE ID = {}".format(driver)) 
        data = curs.fetchone()
        erwarteteAnkunft = regex(data[0])
        startp = changePlaceToGeoC(start)
        stopp = changePlaceToGeoC(stop)
        neueAnkunft = mapRestNeueAnkunft(start, stop)
        neueAnkunft = regex(neueAnkunft)
        if (neueAnkunft > erwarteteAnkunft): 
            enterDataWithStatus(start, startp[0], startp[1], stop, stopp[0], stopp[1], data[0], driver, "drivingNotInTime", neueAnkunft)    
            con.close()
        else:
            enterDataWithStatus(start, startp[0], startp[1], stop, stopp[0], stopp[1], data[0], driver, "driving", neueAnkunft)
            con.close()
    else:
        beendeFahrt()

'''Hue Lampensteuerung'''
def Lampensteuerung(staff, state): 
    if str(state) == "inaktiv":
        requests.put("http://{0}/api/{1}/lights/{2}/state/".format(ip, user, int(staff)), data='{"on": false}')
    elif str(state) == "driving":
        requests.put("http://{0}/api/{1}/lights/{2}/state/".format(ip, user, int(staff)), data='{"on":true, "sat":254, "bri":254, "hue":7983}')
    elif str(state) == "waiting":
        requests.put("http://{0}/api/{1}/lights/{2}/state/".format(ip, user, int(staff)), data='{"on":true,"sat":254,"bri":254,"hue":25500}')
    elif str(state) == "drivingNotInTime":
        requests.put("http://{0}/api/{1}/lights/{2}/state/".format(ip, user, int(staff)), data='{"on" :true,"sat":254,"bri":254,"hue":65535}')
        time.sleep(1)
        requests.put("http://{0}/api/{1}/lights/{2}/state/".format(ip, user, int(staff)), data='{"on": false}')
    else:
        print("error")    

'''aendert den Status und andere Daten in der Datenbank wenn die Fahrt beendet ist'''
def beendeFahrt():
    con = sqlite3.connect(DatabaseFile)
    curs = con.cursor()
    curs.execute("SELECT ID, Status, Zielpunkt, tatsaechlicheZeit FROM stuff")
    data = curs.fetchall()
    x = 0
    for row in data:
        if data[x][1] == "driving" or data[x][1] == "drivingNotInTime":
            if CurrentTimeEqualsDestinationTime(data[x][0]) == True:
                newStatus(data[x][0], "waiting", data[x][2])
                print("Fahrt beendet: " + data[x][1] + "    Fahrer: " + data[x][0])
            elif CurrentTimeEqualsDestinationTime(data[x][0]) == None:
                pass
            elif CurrentTimeEqualsDestinationTime(data[x][0]) == False:
                print("Fahrer: " + str(data[x][0]) + "    Status: " + str(data[x][1]))
        else:
            print("Fahrer: " + str(data[x][0]) + "    Status: " + str(data[x][1]))
        x = x + 1
    print()
    con.close()

'''prueft ob die aktuelle Zeit mit der gewuenschten Ankunftszeit uebereinstimmt.'''
def CurrentTimeEqualsDestinationTime(driver):
    con = sqlite3.connect(DatabaseFile)
    curs = con.cursor()
    curs.execute("SELECT ID, tatsaechlicheZeit FROM stuff")
    data = curs.fetchall()
    for row in data:    
        if driver == row[0]:
            time = regex(row[1]) 
            if str(time) != '#':
                if timeBetweenStartAndStop(time - timedelta(seconds=10), time + timedelta(seconds=10), datetime.now()) == True:
                    con.close()  
                    return True
            elif time == '#':
                pass
            else:
                print("Wrong Time Object")
    con.close() 
    return False


def regex(param):
    time = param
    try:
        time = datetime.strptime(param, '%Y-%m-%dT%H:%M:%S')
    except Exception as e:
        pass 
    try:
        time = datetime.strptime(param, '%Y-%m-%dT%H:%M')
    except Exception as er:
        pass
    try:
        time = datetime.strptime(param, '%Y-%m-%d %H:%M')  
    except Exception as erk:
        pass
    try:
        time = datetime.strptime(param, '%Y-%m-%d %H:%M:%S') 
    except Exception as exp:
        pass   
    return time

'''prueft ob x zwischen start und stop liegt.'''
def timeBetweenStartAndStop(start, end, x):
    return True if (start <= end and start <= x <= end) else False

'''Methoden fuer den Zugriff auf die Datenbank'''

def newInputForDatabase(start, startx, starty, target, zeilx, ziely, time, fahrer):
    con = sqlite3.connect(DatabaseFile)
    curs = con.cursor()
    curs.execute("UPDATE stuff SET Status='{0}', Startpunkt='{1}', startx='{2}', starty='{3}', Zielpunkt='{4}', zeilx='{5}', ziely='{6}', zielZeit='{7}', tatsaechlicheZeit='{9}' WHERE ID={8}".format("driving", start, startx, starty, target, zeilx, ziely, time, int(fahrer), time))
    con.commit()
    con.close() 

def newStatus(driver, status, place):
    print([driver,status,place])
    con = sqlite3.connect(DatabaseFile)
    curs = con.cursor()
    curs.execute("UPDATE stuff SET Status='{0}', Startpunkt='{1}', startx='{2}', starty='{3}', Zielpunkt='{4}', zeilx='{5}', ziely='{6}', zielZeit='{7}', tatsaechlicheZeit='{9}' WHERE ID={8}".format("inaktiv", "-", "-","-", "-", "-", "-", "#", int(driver), "#"))
    curs.execute("UPDATE stuff SET Status='{}', aktuellePosition='{}' WHERE ID={}".format(str(status), str(place), int(driver)))
    con.commit()
    con.close()

def submitionStatus(driver):
    con = sqlite3.connect(DatabaseFile)
    curs = con.cursor()
    curs.execute("SELECT Status FROM stuff WHERE ID = {}".format(driver))
    data = curs.fetchone()
    con.close()
    return True if data[0] == "waiting" else False


def enterDataWithStatus(start, startx, starty, target, zeilx, ziely, time, fahrer, status, timetwo):
    con = sqlite3.connect(DatabaseFile)
    curs = con.cursor()
    curs.execute("UPDATE stuff SET Status='{0}', Startpunkt='{1}', startx='{2}', starty='{3}', Zielpunkt='{4}', zeilx='{5}', ziely='{6}', zielZeit='{7}', tatsaechlicheZeit='{9}' WHERE ID={8}".format(status, start, startx, starty, target, zeilx, ziely, time, int(fahrer), timetwo))
    con.commit()
    con.close()

'''JSON'''
def wrongJsonNoChange():
    return json.loads('{"error": [{"taxifahrer": "korrekt", "status": "ungueltiger Status","aktuellerStandort": "korrekt","Comment": "der Status darf im Moment nicht geaendert werden"}]}')

def wrongJson(ex):
    return json.loads('{"error": [{"taxifahrer": "korrekt","status": "korrekt","aktuellerStandort": "korrekt","Comment": "es wird ein JSON benoetigt"}]}')

def wrongJsonDriver():
    return json.loads('{"error": [{"taxifahrer": "Taxifahrer existiert nicht","status": "korrekt","aktuellerStandort": "korrekt"}]}')

def okJson():
    return json.loads('{"success": [{"taxifahrer": "korrekt","status": "korrekt","aktuellerStandort": "korrekt"}]}')

def wrongJsonStatus():
    return json.loads('{"error": [{"taxifahrer": "korrekt","status": "ungueltiger Status","aktuellerStandort": "korrekt"}]}')

def wrongJsonPlace():
    return json.loads('{"error": [{"taxifahrer": "korrekt","status": "korrekt","aktuellerStandort": "Ort existiert nicht"}]}')