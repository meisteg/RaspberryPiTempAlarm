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

GPIO_SHUTDOWN_BUTTON = 23

# Use the pin numbers from the ribbon cable board
GPIO.setmode(GPIO.BCM)

# Configure GPIO
GPIO.setup(GPIO_SHUTDOWN_BUTTON, GPIO.IN, pull_up_down=GPIO.PUD_UP)

print "Waiting for shutdown button press"
GPIO.wait_for_edge(GPIO_SHUTDOWN_BUTTON, GPIO.FALLING)
print "Shutting down due to button press!"

GPIO.cleanup()
os.system("shutdown -h now")

