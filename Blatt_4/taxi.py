from flask import Flask, render_template, request
import sqlite3
from API_REC_TRANS import *
from API_REC_TRANS import *


app = Flask(__name__)


File = 'drivers.sqlite3'

@app.route("/", methods = ['POST', 'GET'])
def hello():
    if request.method == 'POST':
        result = request.form
        start = result['textfield']
        stop = result['textfield2']
        time = result['textfield3']
        fahr = result['fahr']
        if submitionStatus(fahr) == True:
            startpoint = cood(start)
            stoppoint = cood(stop)
            apiData = getRestAPiData(start, stop, time)
            enterData(start, startpoint[0], startpoint[1], stop, stoppoint[0], stoppoint[1], time, fahr)
            return render_template('index.html', text=start, text2=stop, text3=time, fahr=fahr, apidate=apiData)
        else:
            return render_template('index.html', waning="Cannot use this driver, He/SHE is driving")
    else:
        return render_template('index.html')


@app.route("/rest", methods = ['POST', 'GET'])
def api():
    if request.method == 'POST':
        result = request.form
    return "HELLL"


@app.route("/track", methods = ['POST', 'GET'])
def trackDriver():
    if request.method == 'POST':
        connection_ = sqlite3.connect(File)
        cursor_ = connection_.cursor()
        result = request.form
        fahr = result['fahr']
        if fahr == None:
            return render(request, 'routeTrip.html', {'title': "Kein Ergebnis"})
        cursor_.execute("SELECT * FROM driver WHERE staff_number={} ".format(int(fahr)))
        drive = cursor_.fetchone()
        connection_.commit()
        connection_.close() 
        return render_template('track.html', title=fahr, name=drive[1], status=drive[2], cP=drive[3], start=drive[4], sx=drive[5], sy=drive[6], ziel=drive[7], zx=drive[8], zy=drive[9], time=drive[10])
    return render_template('track.html')

if __name__ == '__main__':
    app.run(debug=True)