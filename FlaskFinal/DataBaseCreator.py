'''Nam Pham, Paul Schroeder'''
import sqlite3,sys
from time import sleep

'''Datenbank fuer Taxifahrer'''
DatabaseFile = "t4.sqlite3"

'''leert die Datenbank'''
def dropTable():
    con = sqlite3.connect(DatabaseFile)
    curs = con.cursor()
    curs.execute(""" DROP TABLE IF EXISTS stuff """)
    con.commit()
    con.close()

'''initialisiert die Datenbank mit gueltigen Startwerten'''
def dataBaseTableCreater():
    con = sqlite3.connect(DatabaseFile)
    curs = con.cursor()
    curs.execute('''CREATE TABLE IF NOT EXISTS stuff (ID INTEGER PRIMARY KEY, Name text,  Status text, aktuellePosition text, Startpunkt text, startx text,  starty text, Zielpunkt text, zeilx text, ziely text, zielZeit text, tatsaechlicheZeit text);''')                                                                                                                                       
    curs.execute('''INSERT INTO stuff (Name, Status, aktuellePosition, Startpunkt, startx, starty, Zielpunkt, zeilx, ziely, zielZeit, tatsaechlicheZeit) VALUES ('TIM', 'waiting','Berlin', '-','-', '-', '-', '-', '-', '#', '#')''')
    curs.execute('''INSERT INTO stuff (Name, Status, aktuellePosition, Startpunkt, startx, starty, Zielpunkt, zeilx, ziely, zielZeit, tatsaechlicheZeit) VALUES ('KARL', 'waiting','Rom', '-','-', '-', '-', '-', '-', '#', '#')''')
    curs.execute('''INSERT INTO stuff (Name, Status, aktuellePosition, Startpunkt, startx, starty, Zielpunkt, zeilx, ziely, zielZeit, tatsaechlicheZeit) VALUES ('JOHANN', 'waiting','Paris', '-','-', '-', '-', '-', '-', '#', '#')''')
    con.commit()
    con.close()
 
'''gibt die aktuelle Datenbank aus''' 
def printer():
    con = sqlite3.connect(DatabaseFile)
    curs = con.cursor()
    curs.execute("SELECT * FROM stuff")
    data = curs.fetchall()
    for x in data:
        print(x)
    con.close()

#dropTable()
#dataBaseTableCreater()
printer()