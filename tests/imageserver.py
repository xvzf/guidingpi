"""
GUIDINGPI

Copyright (C) 2017 Matthias Riegler <matthias@xvzf.tech>
Copyright (C) 2017 Dominik Laa      <dominik@xvzf.tech>

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
"""

import socket
import time
from picamera import PiCamera
from fractions import Fraction
import os
 
class AstroServer(object):
    """docstring for AstroServer"""
    # RPi camera presets
    #===================================================================================
    rpimodes =  ['off',  'auto', 'night', 'night', 'sports', 'sports', 'verylong', 'verylong', 'fireworks', 'fireworks']
    rpimodesa = [' off', 'auto', 'night', 'nigh2', 'sport',  'spor2',  'vlong',    'vlon2',    'fwork',     'fwor2']
 

    def __init__(self):
        super(AstroServer, self).__init__()
        # Force sensor mode 4 (for 2x2 binned images), set
        # the framerate to any value
        self.camera = PiCamera(
            resolution=(1296, 972),
            sensor_mode=4)
 
        self.camera.sharpness = 0
        self.camera.contrast = 90
        self.camera.brightness = 76
        self.camera.saturation = 0
        self.camera.ISO = 800
        self.camera.video_stabilization = False
        self.camera.exposure_compensation = 0
        self.camera.exposure_mode = 'night'
        self.camera.meter_mode = 'average'
        self.camera.awb_mode = 'auto'
        self.camera.image_effect = 'none'
        self.camera.color_effects = None
        self.camera.rotation = 0
        self.camera.hflip = False
        self.camera.vflip = False
        self.camera.crop = (0.0, 0.0, 1.0, 1.0)
        self.camera.framerate = Fraction(1, 3)
        print("self.camera setting applied - wait 30 seconds for self.camera to warm up")
        time.sleep(15) # should be way enough!
        self.camera.shutter_speed = 3000000
        # Setup Socket
        self.setupsocket()
 

    def setupsocket(self):
        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)  # Create a socket object
        port = 8001                 # Reserve a port for your service.
        self.server.bind(("0.0.0.0", port))   # wait for incoming connection
 

    def sendimage(self, connection):
        print("Start sending")
        f = open('/tmp/test.jpg', 'rb') # /tmp is the standart dir for temporary files, shm is obsolete
        #l = f.read(1024)
        #if l:
        connection.sendall(f.read())
        f.close()
        print("Sending Done.")
 

    def recordimage(self):
        try:
            os.remove('/tmp/test.jpg')
        except OSError:
            pass
        self.camera.capture('/tmp/test.jpg')
        print("Capture of image done")
 

    def startlistening(self):
        try:
            self.server.listen(1)                 # Now wait for client connection.
            while True:
                print("Waiting for a connection...")
                conn, addr = self.server.accept() # c usually used for client ;)
                print("Connection from: " + str(addr))
                self.recordimage()
                self.sendimage(conn)
                conn.close()
        except Exception as e:
            raise e
        finally:
            self.server.close()
 


def main():
    a = AstroServer()
    a.startlistening()
 
if __name__ == '__main__':
    main()