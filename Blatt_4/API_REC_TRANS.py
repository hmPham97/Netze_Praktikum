import requests,sqlite3,time,datetime,json, objectpath, traceback, atexit
from apscheduler.schedulers.background import BackgroundScheduler
from threading import Thread
from background_task import background
from datetime import datetime,timedelta
from dateutil.parser import parse

rest_ID = "Jn1hTy9VwYbWrKb7D1My"
rest_Code = "GB3AZ67KqDfqt0HIx1nG8Q"

File = 'drivers.sqlite3'

def getCoordinats(point):
    reqeustResult = requests.get("https://geocoder.api.here.com/6.2/geocode.json?app_id={0}&app_code={1}&searchtext={2}".format(rest_ID, rest_Code, point)).json()
    result = json.dumps(reqeustResult)
    jsonReady = json.loads(result)
    x_Coo =  jsonReady['Response']['View'][0]['Result'][0]['Location']['DisplayPosition']['Latitude']
    y_Coo =  jsonReady['Response']['View'][0]['Result'][0]['Location']['DisplayPosition']['Longitude']
    return [x_Coo, y_Coo]

def extractor(time):
    return (int(time[0:4]), int(time[5:7]), int(time[8:10]), int(time[11:13]), int(time[14:16]))

def getRestAPiData(start, stop, time):
    startpoint = getCoordinats(start)
    stoppoint = getCoordinats(stop)
    reqeustResult = requests.get("https://route.api.here.com/routing/7.2/calculateroute.json?app_id={0}&app_code={1}&waypoint0=geo!{2},{3}&waypoint1=geo!{4},{5}&mode=fastest;car;traffic:enabled".format(rest_ID, rest_Code, startpoint[0], startpoint[1], stoppoint[0], stoppoint[1])).json()
    result = json.dumps(reqeustResult)
    jsonReady = json.loads(result)
    json_tree = objectpath.Tree(jsonReady['response'])
    result_tuple = tuple(json_tree.execute('$..travelTime'))
    rTT = max(result_tuple)
    date = 0
    tup = extractor(time)
    date = datetime.datetime(tup[0], tup[1], tup[2], tup[3], tup[4], 00)
    aTT = date -  datetime.timedelta(seconds=rTT) 
    return [rTT,aTT]

def enterData(start, s_x, s_y, target, t_x, t_y, time, fahrer):
    connector = sqlite3.connect(File)
    myCursor = connector.cursor() #Name, Status, currentPlace, startPointCurrentTour, s_x, s_y, zielPointCurrentTour, z_x, z_y
    myCursor.execute("UPDATE driver SET Status='{0}', startPointCurrentTour='{1}', s_x='{2}', s_y='{3}', zielPointCurrentTour='{4}', z_x='{5}', z_y='{6}', targTime='{7}' WHERE staff_number={8}".format("driving", start, s_x, s_y, target, t_x, t_y, time, int(fahrer)))
    drive = myCursor.fetchone()
    connector.commit()
    connector.close()

def changeStatusInDatabase(driver, status, place):
    print([driver,status,place])
    connector = sqlite3.connect(File)
    myCursor = connector.cursor()
    myCursor.execute("UPDATE driver SET Status='{0}', startPointCurrentTour='{1}', s_x='{2}', s_y='{3}', zielPointCurrentTour='{4}', z_x='{5}', z_y='{6}', targTime='{7}' WHERE staff_number={8}".format("inaktiv", "undef", "undef","undef", "undef", "undef", "undef", "00", int(driver)))
    myCursor.execute("UPDATE driver SET Status='{}', currentPlace='{}' WHERE staff_number={}".format(str(status), str(place), int(driver)))
    drive = myCursor.fetchone()
    connector.commit()
    connector.close()
    return True

def submitionStatus(driver):
    connector = sqlite3.connect(File)
    myCursor = connector.cursor()
    myCursor.execute("SELECT Status FROM driver WHERE staff_number = {}".format(driver))
    print("DRIVER: " + str(driver))
    row = myCursor.fetchone()
    print("ROW: " + str(row))
    connector.commit()
    connector.close()
    if row[0] == "waiting":
        return True
    else:
        return False

def getMini(start, stop):
    startpoint = getCoordinats(start)
    stoppoint = getCoordinats(stop)
    reqeustResult = requests.get("https://route.api.here.com/routing/7.2/calculateroute.json?app_id={0}&app_code={1}&waypoint0=geo!{2},{3}&waypoint1=geo!{4},{5}&mode=fastest;car;traffic:enabled".format(rest_ID, rest_Code, startpoint[0], startpoint[1], stoppoint[0], stoppoint[1])).json()
    result = json.dumps(reqeustResult)
    jsonReady = json.loads(result)
    json_tree = objectpath.Tree(jsonReady['response'])
    result_tuple = tuple(json_tree.execute('$..travelTime'))
    rTT = max(result_tuple)
    tup = datetime.datetime.now()
    return extractorReverse(str(tup +  datetime.timedelta(seconds=rTT)))

def extractorReverse(time):
    return str(int(time[0:4] )) + "-" + str(int(time[5:7])) + "-" + str(int(time[8:10])) + "T" + str(int(time[11:13])) + ":" + str(int(time[14:16]))


def checkGivenPosition(position):
    jsonReady = json.dumps(requests.get("https://geocoder.api.here.com/6.2/geocode.json?app_id={0}&app_code={1}&searchtext={2}".format(rest_ID, rest_Code, position)).json())
    return "Result" in jsonReady or "result" in jsonReady
   
def wrongJSONCANNOtChange():
	return json.loads('{"error": [{"Driver": "OK", "Status": "False Illegal State","CurrentPlace": "OK","Comment": "Cannot change the Status to driving by a Worker"}]}')

def wrongJSON(ex):
	return json.loads('{"error": [{"Driver": "OK","Status": "OK","CurrentPlace": "OK","Comment": "JSON FORMAT ERROR - UNKNOWN FORMAT - NEED JSON"}]}')

def getWrongDriver():
	return json.loads('{"error": [{"Driver": "wrong driver","Status": "OK","CurrrentPlace": "OK"}]}')

def getWorkedJSON():
	return json.loads('{"success": [{"Driver": "OK","Status": "OK","CurrrentPlace": "OK"}]}')

def getUnkownStatusJSON():
	return json.loads('{"error": [{"Driver": "OK","Status": "wrong status","CurrrentPlace": "OK"}]}')

def getWrongPlaceJSON():
	return json.loads('{"error": [{"Driver": "OK","Status": "OK","CurrrentPlace": "unknown place"}]}')

def apiReader(requ):
	try :
		jsn = json.loads(requ.body)
		driver = jsn['Driver']
		status = jsn['Status']
		place = jsn['currentPlace']
		connector = sqlite3.connect(File)
		myCursor = connector.cursor()
		myCursor.execute("SELECT staff_number, Status, zielPointCurrentTour FROM driver")
		rows = myCursor.fetchall()
		for row in rows:
			if (int(driver) == int(row[0])):
				if (status == "driving" or status == "drivingNotInTime" or status == "waiting" or status == "inaktiv"):
					if (checkGivenPosition(str(place)) == True):
						if row[1] != "driving" and row[1] != "drivingNotInTime":
							if status != "driving" and status != "drivingNotInTime":
								changeStatusInDatabase(driver, status, place)
								return getWorkedJSON()
							else:
								return wrongJSONCANNOtChange()
						elif row[1] == "driving" or row[1] == "drivingNotInTime":
							try:
								updatePositionForStatusDrivingAndNIT(place, row[2], driver)
							except Exception as e:
								return getWrongPlaceJSON()
							return getWorkedJSON()
					else:
						return getWrongPlaceJSON()
				else:
					return getUnkownStatusJSON()
		return getWrongDriver() 
	except Exception as e:
		print(traceback.format_exc())
		return wrongJSON(e)

def updatePositionForStatusDrivingAndNIT(start, stop, driver):
	if (start != stop):
		connector = sqlite3.connect(File)
		myCursor = connector.cursor()
		myCursor.execute("SELECT targTime FROM driver WHERE staff_number = {}".format(driver)) 
		ro = myCursor.fetchone()
		estimatedArrivalTime = datetime.strptime(ro[0], '%Y-%m-%dT%H:%M') if ('T' in str(ro[0])) else datetime.strptime(ro[0], '%Y-%m-%d %H:%M')
		startpoint = getCoordinats(start)
		stoppoint = getCoordinats(stop)
		calculatedTime = getMini(start, stop)
		calculatedTime = datetime.strptime(calculatedTime, '%Y-%m-%dT%H:%M') if ('T' in str(calculatedTime)) else datetime.strptime(calculatedTime, '%Y-%m-%d %H:%M')
		if (calculatedTime > estimatedArrivalTime): 
			enterDataWithStatus(start, startpoint[0], startpoint[1], stop, stoppoint[0], stoppoint[1], ro[0], driver, "drivingNotInTime", calculatedTime.isoformat())    
			connector.close()
			return "Not in Time"
		else:
			enterDataWithStatus(start, startpoint[0], startpoint[1], stop, stoppoint[0], stoppoint[1], ro[0], driver, "driving", calculatedTime.isoformat())
			connector.close()
			return "In Time"
	else:
		finishDrive()
		return "Finish Driving"

ip = "localhost:80" 
user = "newdeveloper"

#ip = "10.28.209.13:9001"
#user = "3dc1d8f23e55321f3c049c03ac88dff" # --> platz 2/Bridge 2 

def checkState(staff, state):
    if str(state) == "inaktiv":
        requests.put("http://{0}/api/{1}/lights/{2}/state/".format(ip, user, int(staff)), data='{"on": false}')
    elif str(state) == "driving":
        requests.put("http://{0}/api/{1}/lights/{2}/state/".format(ip, user, int(staff)), data='{"on":true, "sat":254, "bri":254, "hue":7983}')
    elif str(state) == "waiting":
        requests.put("http://{0}/api/{1}/lights/{2}/state/".format(ip, user, int(staff)), data='{"on":true,"sat":254,"bri":254,"hue":25500}')
    elif str(state) == "drivingNotInTime":
        requests.put("http://{0}/api/{1}/lights/{2}/state/".format(ip, user, int(staff)), data='{"on" :true,"sat":254,"bri":254,"hue":65535}')
        requests.put("http://{0}/api/{1}/lights/{2}/state/".format(ip, user, int(staff)), data='{"on": false}')

def finishDrive():
    connector = sqlite3.connect(File)
    myCursor = connector.cursor()
    myCursor.execute("SELECT staff_number, Status, zielPointCurrentTour, actuallTime FROM driver")
    rows = myCursor.fetchall()
    x = 0
    for row in rows:
        if rows[x][1] == "driving" or rows[x][1] == "drivingNotInTime":
            if timeKeeper(rows[x][0]) == True:
                changeStatusInDatabase(rows[x][0], "waiting", rows[x][2])
                print(str(rows[x][0]))
            elif timeKeeper(rows[x][0]) == None:
                pass
            elif timeKeeper(rows[x][0]) == False:
                print("No changes yet")
        else:
            print("Nothing to change")
        x = x + 1
    connector.commit()
    connector.close()

def timeKeeper(driver):
    connector = sqlite3.connect(File)
    myCursor = connector.cursor()
    myCursor.execute("SELECT staff_number, actuallTime FROM driver")
    arr = myCursor.fetchall()
    for row in arr:    
        if driver == row[0]:
            time = datetime.strptime(row[1], '%Y-%m-%dT%H:%M')
            if time_in_range(time - timedelta(seconds=20), time + timedelta(seconds=20), datetime.now()):
                connector.commit()
                connector.close()  
                return True
    connector.commit()
    connector.close() 
    return False

def time_in_range(start, end, x):
    if start <= end:
        return start <= x <= end
    else:
        return start <= x or x <= end

def sendPUT():
    connector = sqlite3.connect(File)
    myCursor = connector.cursor()
    myCursor.execute("SELECT staff_number, Status FROM driver")
    rows = myCursor.fetchall()
    x = 0
    for row in rows:
        checkState(rows[x][0], rows[x][1])
        x = x + 1
    connector.commit()
    connector.close()
    time.sleep(3.5)
    finishDrive()

scheduler = BackgroundScheduler()
scheduler.add_job(func=sendPUT, trigger="interval", seconds=1)
scheduler.start()
atexit.register(lambda: scheduler.shutdown())