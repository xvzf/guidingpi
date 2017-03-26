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
import socketserver

 
class AstroServer(socketserver.BaseRequestHandler):
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
        

    def handle(self):
        commandid = int.from_bytes(self.request.recv(4), byteorder='big')
        # Add control interfacing options here
        if commandid == 1:
            self.camera.shutter_speed = int.from_bytes(self.request.recv(4), byteorder='big') * 1000
            print("shutter speed set to: " + str(self.camera.shutter_speed))
        elif commandid == 2:
            val = pack('!i', self.camera.shutter_speed/1000)
            self.request.send(val)
        elif commandid == 3:
            self.camera.ISO = int.from_bytes(self.request.recv(4), byteorder='big')
            print("iso set to: " + str(self.camera.ISO))
        elif commandid == 4:
            val = pack('!i', self.camera.iso)
            self.request.send(val)

def __exit__(self, type, value, tb):
    print("closing programm")

def main():
    HOST, PORT = "0.0.0.0", 9999

    # Create the server, binding to localhost on port 9999
    with socketserver.TCPServer((HOST, PORT), AstroServer) as server:
        # Activate the server; this will keep running until you
        # interrupt the program with Ctrl-C
        server.serve_forever()
 
if __name__ == '__main__':
    main()
