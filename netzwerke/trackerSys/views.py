from django.shortcuts import render
from django.http import HttpResponse
from django.core.exceptions import *
import requests
import json
import objectpath
import datetime

#install onjectpath

#API 
rest_ID = "Jn1hTy9VwYbWrKb7D1My"
rest_Code = "GB3AZ67KqDfqt0HIx1nG8Q"

'''
Will return a Index.html Template
'''
def index(request):
    return render(request, 'index.html')


'''
Will return a Search.html Template 
'''
def search(request):
    return render(request, 'index.html')


'''

'''
def submition(request):
    start = request.POST.get('textfield', None)
    stop = request.POST.get('textfield2', None)
    time = request.POST.get('textfield3', None)
    fahr = request.POST.get('fahr')
    fahr = driver(fahr)
    apiData = getRestAPiData(start, stop, time)
    args = {'text':start, 'text2': stop, 'text3':time, 'fahr':fahr, 'api-date':apiData}
    return render(request, 'search.html', args)


'''
Method change the name of the Driver.
'''
def driver(param):
    if (int(param)) == 1:
        param = "Fahrer 1"
    elif (int(param)) == 2:
        param = "Fahrer 2"
    elif (int(param)) == 3:
        param = "Fahrer 3"
    else:
        param = "Dont try to break into my System"
    return param

'''
Will get the Data from the API and will return the StartTime
'''
def getRestAPiData(start, stop, time):
    startpoint = cood(start)
    stoppoint = cood(stop)
    reqeustResult = requests.get("https://route.api.here.com/routing/7.2/calculateroute.json?app_id={0}&app_code={1}&waypoint0=geo!{2},{3}&waypoint1=geo!{4},{5}&mode=fastest;car;traffic:enabled".format(rest_ID, rest_Code, startpoint[0], startpoint[1], stoppoint[0], stoppoint[1])).json()
    result = json.dumps(reqeustResult)
    jsonReady = json.loads(result)
    json_tree = objectpath.Tree(jsonReady['response'])
    result_tuple = tuple(json_tree.execute('$..travelTime'))
    rTT = max(result_tuple)
    date = 0
    tup = extractor(time)
    print(rTT)
    date = datetime.datetime(tup[0], tup[1], tup[2], tup[3], tup[4], 00)
    aTT = date -  datetime.timedelta(seconds=rTT) 
    print([rTT,aTT])
    return [rTT,aTT]


def extractor(time):
    year = time[0:4] 
    month = time[5:7]
    day = time[8:10]
    hours= time[11:13]
    minutes = time[14:16]
    return (int(year), int(month), int(day), int(hours), int(minutes))


'''
Method to get the Coordinate from a given point.
Example Munich [48.13642,11.57755]
Exmaple Berlin [52.51605, 13.37691]
'''
def cood(point):
    reqeustResult = requests.get("https://geocoder.api.here.com/6.2/geocode.json?app_id={0}&app_code={1}&searchtext={2}".format(rest_ID, rest_Code, point)).json()
    result = json.dumps(reqeustResult)
    jsonReady = json.loads(result)
    x_Coo =  jsonReady['Response']['View'][0]['Result'][0]['Location']['DisplayPosition']['Latitude']
    y_Coo =  jsonReady['Response']['View'][0]['Result'][0]['Location']['DisplayPosition']['Longitude']
    return [x_Coo, y_Coo]



def driverTracker(request):
    return render(request, 'change.html')