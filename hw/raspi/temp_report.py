#!/usr/bin/env python

# Copyright (C) 2014 Gregory S. Meiste  <http://gregmeiste.com>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

try:
  from apiclient.discovery import build
except ImportError:
  print "error: apiclient is not installed. Please run the following command:"
  print "       sudo pip install --upgrade google-api-python-client"
  sys.exit(1)

from datetime import datetime

import glob
import httplib2
import os
import sys
import time
import threading

import RPi.GPIO as GPIO

DEBUG = False

SERVER_URL = "https://rasptempalarm.appspot.com"
SERVER_API = "temperature"
SERVER_API_VER = "v1"

MIN_PYTHON_VERSION = (2, 6)     # minimum supported python version

GPIO_PHOTOCELL = 12
GPIO_RED_LED = 20
GPIO_GREEN_LED = 21
GPIO_QUIT_BUTTON = 24

# Python version check
ver = sys.version_info
if (ver[0] == 3) or ((ver[0], ver[1]) < MIN_PYTHON_VERSION):
  print("error: Python version %s is unsupported. "
        "Please use Python 2.6 - 2.7 instead."
        % sys.version.split(' ')[0])
  sys.exit(1)

sensor_file = ""
endpoint = None
report_rate = 60
condition = threading.Condition()
should_quit = False

# Setup print to add timestamp to each print
old_f = sys.stdout
class F:
  nl = True
  def write(self, x):
    if x == '\n':
      old_f.write(x)
      self.nl = True
    elif self.nl == True:
      old_f.write("[%s] %s" % (str(datetime.now()), x))
      self.nl = False
    else:
      old_f.write(x)
sys.stdout = F()

def quit_callback(channel):
  global should_quit

  condition.acquire()
  print "Quit button pressed!"

  # To prevent phantom stoppages due to ESD, need to verify button held a
  # reasonable period of time
  time.sleep(0.2)
  should_quit = GPIO.input(channel) == GPIO.LOW

  if should_quit:
    condition.notify()
  else:
    print "False button press. Ignoring..."

  condition.release()

def read_temp_raw():
  f = open(sensor_file, 'r')
  lines = f.readlines()
  f.close()
  return lines

def read_temp():
  lines = read_temp_raw()
  while lines[0].strip()[-3:] != 'YES':
    time.sleep(0.2)
    lines = read_temp_raw()
  equals_pos = lines[1].find('t=')
  if equals_pos != -1:
    temp_string = lines[1][equals_pos+2:]
    temp_c = float(temp_string) / 1000.0
    temp_f = temp_c * 9.0 / 5.0 + 32.0
    return temp_f

def read_light():
  reading = 0

  GPIO.setup(GPIO_PHOTOCELL, GPIO.OUT)
  GPIO.output(GPIO_PHOTOCELL, GPIO.LOW)
  time.sleep(0.1)
  
  GPIO.setup(GPIO_PHOTOCELL, GPIO.IN)
  while (GPIO.input(GPIO_PHOTOCELL) == GPIO.LOW):
    reading += 1
  return reading

def setLedSuccess(success):
  GPIO.output(GPIO_RED_LED, not success)
  GPIO.output(GPIO_GREEN_LED, success)

def init():
  global sensor_file
  global endpoint
  global report_rate

  # Use the pin numbers from the ribbon cable board
  GPIO.setmode(GPIO.BCM)

  # Configure GPIOs
  GPIO.setup(GPIO_QUIT_BUTTON, GPIO.IN, pull_up_down=GPIO.PUD_UP)
  GPIO.setup(GPIO_GREEN_LED, GPIO.OUT)
  GPIO.setup(GPIO_RED_LED, GPIO.OUT)

  # Set GPIOs to known states
  GPIO.output(GPIO_RED_LED, GPIO.LOW)
  GPIO.output(GPIO_GREEN_LED, GPIO.LOW)

  print "Loading kernel modules"
  if os.system('modprobe w1-gpio'):
    print "error: Failed to load w1-gpio"
    return -1
  if os.system('modprobe w1-therm'):
    print "error: Failed to load w1-therm"
    return -1

  # Give modules time to initialize and find sensors on bus
  time.sleep(1)

  base_dir = '/sys/bus/w1/devices/'
  sensors = glob.glob(base_dir + '28*')
  if not sensors:
    print "error: No sensors found!"
    return -1
  sensor_file = sensors[0] + '/w1_slave'
  print "Sensor found:", sensor_file

  print "Initializing server API"
  http = httplib2.Http()
  try:
    endpoint = build(SERVER_API, SERVER_API_VER, http=http,
                    discoveryServiceUrl=(SERVER_URL + "/_ah/api/discovery/v1/apis/" +
                                         SERVER_API + "/" + SERVER_API_VER + "/rest"))

    if DEBUG:
      print "Using accelerated report rate for debugging"
      report_rate = "5"
    else:
      print "Retrieve desired report rate from backend"
      report_rate = endpoint.getReportRate().execute()["value"]
      print "Backend requests report rate of", report_rate, "seconds"
  except Exception as e:
    print "error:", e.__doc__
    return -1

  # Add interrupt
  GPIO.add_event_detect(GPIO_QUIT_BUTTON, GPIO.FALLING,
                        callback=quit_callback, bouncetime=300)

  # Init complete!
  setLedSuccess(True)
  return 0

def main():
  while init():
    setLedSuccess(False)
    time.sleep(5)

  condition.acquire()
  try:
    while not should_quit:
      temp_f = read_temp()
      light = read_light()
      print "Reporting", temp_f, "degrees and", light, "light to the backend"
      try:
        endpoint.report(temperature=temp_f, light=light).execute()
        setLedSuccess(True)
        condition.wait(float(report_rate))
      except Exception as e:
        print "error:", e.__doc__
        setLedSuccess(False)
        condition.wait(30)

    print "Stopping power alarm on server"
    try:
      endpoint.stop().execute()
    except Exception as e:
      print "error:", e.__doc__
  finally:
    condition.release()

  GPIO.cleanup()
  print "DONE"

if __name__ == "__main__":
  main()

