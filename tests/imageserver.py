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
import struct
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
        # TODO: add support for the v1 camera module
        self.camera = PiCamera(
            resolution=(1640, 1232),
            sensor_mode=4)

        # apply default settings
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
        time.sleep(15)
        self.camera.shutter_speed = 3000000 # = 3sec
        
        # setup image socket
        self.setupimagesocket()

        # setup control socket
        self.setupcontrolsocket()

        # start image thread
        start_new_thread(AstroServer.updatethread, (self,))

        # start control thread
        start_new_thread(AstroServer.updatecontrolthread, (self,))

    def setupimagesocket(self):
        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)     # Create a socket object
        port = 8001                                                         # Reserve a port for your service.
        self.server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)   # allow socket overriding
        self.server.bind(("0.0.0.0", port))                                 # wait for incoming connection

    def setupcontrolsocket(self):
        self.control = socket.socket(socket.AF_INET, socket.SOCK_STREAM)    # Create a socket object
        port = 8002                                                         # Reserve a port for your service.
        self.control.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)  # allow socket overriding
        self.control.bind(("0.0.0.0", port))                                # wait for incoming connection

    def sendimage(self, connection):
        print("Start sending")                          # info message

        f = open("/tmp/" + self.captures[-1], 'rb')     # open image
        connection.sendall(f.read())                    # send complete image
        f.close()                                       # close image

        print("Sending Done.")                          # info message

    def newimg(self):
        filename = datetime.now().strftime("%H:%M:%S:")+str(datetime.now().microsecond)

        # safe the exact time as image id as exif information
        self.camera.exif_tags['EXIF.ImageUniqueID'] = filename

        # record image with the video port - because the denoise algorithm of the 
        # stillimage port is ways to slow (takes up to 10 seconds for a 3 seconds image)
        # use jpeg as format
        self.camera.capture("/tmp/" + filename, format='jpeg', use_video_port=True)

        self.captures.append(filename)

        while len(self.captures) > self.keepcaps:
            try:
                os.remove("/tmp/" + self.captures.pop(0))
            except OSError:
                pass

        print(filename)

    def updatethread(self):
        # keep capturing new images
        while True:
            self.newimg()

    def updatecontrolthread(self):
        # keep waiting for request on the control port
        try:
            self.control.listen(1)
            while True:
                conn, addr = self.control.accept()  # c usually used for client ;)
                print("Waiting for a connection on the control port...")
                commandid = int.from_bytes(conn.recv(4), byteorder='big')
                print("Connection from: " + str(addr) + "[control port] -> Command:" + str(commandid))
                # Add control interfacing options here
                if commandid == 1:
                    self.camera.shutter_speed = int.from_bytes(conn.recv(4), byteorder='big')*1000
                    print("shutter speed set to: "+str(self.camera.shutter_speed))
                elif commandid == 2:
                    conn.send(struct.unpack("4b", struct.pack("I", self.camera.shutter_speed)))
                elif commandid == 3:
                    self.camera.ISO = int.from_bytes(conn.recv(4), byteorder='big')
                    print("iso set to: " + str(self.camera.ISO))
                elif commandid == 4:
                    val = pack('!i', self.camera.iso)
                    conn.send(val)

                conn.close()
        except Exception as e:
            raise e
        finally:
            self.control.close()

    def startlistening(self):
        try:
            self.server.listen(1)                 # Now wait for client connection.
            while True:
                print("Waiting for a connection on the image port...")
                conn, addr = self.server.accept()   # c usually used for client ;)
                timestamp = str(self.captures[-1])  # generate timestamp from the filename
                print("Connection from: " + str(addr) + " sending: " + timestamp)
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
