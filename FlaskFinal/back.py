'''Nam Pham'''

import requests,sqlite3,time,datetime,json, objectpath, traceback, atexit
from datetime import datetime,timedelta
from API_REC_TRANS import *

'''Datenbank fuer Taxifahrer'''
DatabaseFile = 't4.sqlite3'

'''BackgroundTask'''
def backGroundT():
    con = sqlite3.connect(DatabaseFile)
    curs = con.cursor()
    curs.execute("SELECT ID, Status FROM stuff")
    data = curs.fetchall()
    for row in data:
        Lampensteuerung(row[0], row[1])
    con.close()
    beendeFahrt()

while(True):
    backGroundT()
    time.sleep(4.5)