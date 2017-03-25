"""
GUIDINGPI

Copyright (C) 2017 Matthias Riegler <matthias@xvzf.tech>
Copyright (C) 2017 Dominik  Laa     <dominik@xvzf.tech>

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
"""

import socket
import time
from picamera import PiCamera
from fractions import Fraction
from _thread import start_new_thread
from datetime import datetime
import os
 
class AstroServer(object):
    """docstring for AstroServer"""
    # RPi camera presets
    #===================================================================================
    rpimodes =  ['off',  'auto', 'night', 'night', 'sports', 'sports', 'verylong', 'verylong', 'fireworks', 'fireworks']
    rpimodesa = [' off', 'auto', 'night', 'nigh2', 'sport',  'spor2',  'vlong',    'vlon2',    'fwork',     'fwor2']
 
    captures = []
    keepcaps = 3

    def __init__(self):
        super(AstroServer, self).__init__()
        # Force sensor mode 4 (for 2x2 binned images)
        # we can also try 4x4 binned mode but that's only supported by
        # the v1 module
        self.camera = PiCamera(
            resolution=(1640, 1232),
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
        print("self.camera setting applied - wait 15 seconds to get right gain values")
        time.sleep(15) # should be way enough!
        self.camera.shutter_speed = 3000000 #3000000 = 3sec
        
        # Setup Socket
        self.setupsocket()
        
        start_new_thread(AstroServer.updatethread, (self,))
 

    def setupsocket(self):
        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)  # Create a socket object
        port = 8001                             # Reserve a port for your service.
        self.server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) # allow socket overriding
        self.server.bind(("0.0.0.0", port))     # wait for incoming connection
 

    def sendimage(self, connection):
        print("Start sending")
        f = open("/tmp/" + self.captures[-1], 'rb')

        connection.sendall(f.read())
        f.close()
        print("Sending Done.")
 

    def newimg(self):
        
        filename = "astropi_" + datetime.now().strftime('%Y-%m-%d_%H-%M-%S') + ".jpg
        # record image with the video port - because the denoise algorithm of the 
        # stillimage port is ways to slow (takes up to 10 seconds for a 3 seconds image)
        self.camera.capture( "/tmp/" + filename, use_video_port=True) 

        self.captures.append(filename)

        while len(self.captures) > self.keepcaps:
            try:
                os.remove("/tmp/" + self.captures.pop(0))
            except OSError:
                pass

        print(filename)


    def updatethread(self):
        while True:
            self.newimg()


    def startlistening(self):
        try:
            self.server.listen(1)                 # Now wait for client connection.
            while True:
                print("Waiting for a connection...")
                conn, addr = self.server.accept() # c usually used for client ;)
                print("Connection from: " + str(addr))
                #self.newimg()
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
