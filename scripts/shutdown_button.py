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

import RPi.GPIO as GPIO
import os
import time
import threading

GPIO_SHUTDOWN_BUTTON = 23

condition = threading.Condition()

def shutdown_callback(channel):
  condition.acquire()
  print "Shutdown button pressed!"

  # To prevent phantom stoppages due to ESD, need to verify button held a
  # reasonable period of time
  time.sleep(0.2)

  if GPIO.input(channel) == GPIO.LOW:
    condition.notify()
  else:
    print "False button press. Ignoring..."

  condition.release()

def main():
  condition.acquire()

  # Use the pin numbers from the ribbon cable board
  GPIO.setmode(GPIO.BCM)

  # Configure GPIO
  GPIO.setup(GPIO_SHUTDOWN_BUTTON, GPIO.IN, pull_up_down=GPIO.PUD_UP)

  # Add interrupt
  GPIO.add_event_detect(GPIO_SHUTDOWN_BUTTON, GPIO.FALLING,
                        callback=shutdown_callback, bouncetime=300)

  print "Waiting for shutdown button press"
  condition.wait()
  print "Shutting down due to button press!"

  GPIO.cleanup()
  os.system("shutdown -h now")
  condition.release()

if __name__ == "__main__":
  main()

