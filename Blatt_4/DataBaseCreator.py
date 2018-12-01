import sqlite3,sys

File = "drivers.sqlite3"

def dropTable():
    connector = sqlite3.connect(File)
    myCursor = connector.cursor()
    myCursor.execute(""" DROP TABLE IF EXISTS driver; """)
    connector.commit()
    connector.close()

def dataBaseTableCreater():
    connector = sqlite3.connect(File)
    myCursor = connector.cursor()
    myCursor.execute('''CREATE TABLE IF NOT EXISTS driver (staff_number INTEGER PRIMARY KEY, Name text,  Status text, currentPlace text, startPointCurrentTour text, s_x text,  s_y text, zielPointCurrentTour text, z_x text, z_y text, targTime text);''')
    myCursor.execute('''INSERT INTO driver (Name, Status, currentPlace, startPointCurrentTour, s_x, s_y, zielPointCurrentTour, z_x, z_y, targTime) VALUES ('ANNA', 'waiting','munich', 'undef','undef', 'undef', 'undef', 'undef', 'undef', '00')''')
    myCursor.execute('''INSERT INTO driver (Name, Status, currentPlace, startPointCurrentTour, s_x, s_y, zielPointCurrentTour, z_x, z_y, targTime) VALUES ('HANNA', 'waiting','hamburg', 'undef','undef', 'undef', 'undef', 'undef', 'undef', '00')''')
    myCursor.execute('''INSERT INTO driver (Name, Status, currentPlace, startPointCurrentTour, s_x, s_y, zielPointCurrentTour, z_x, z_y, targTime) VALUES ('RUDOLF', 'waiting','berlin', 'undef','undef', 'undef', 'undef', 'undef', 'undef', '00')''')
    myCursor.execute("SELECT * FROM driver")
    connector.commit()
    connector.close()

def printTableOnCMD():
    connector = sqlite3.connect(File)
    myCursor = connector.cursor()
    myCursor.execute("SELECT * FROM driver")
    rows = myCursor.fetchall()
    for row in rows:
        print(str(row))
    connector.commit()
    connector.close()


#dropTable()
#dataBaseTableCreater()
printTableOnCMD()

