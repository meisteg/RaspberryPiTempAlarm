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

import httplib2
import sys

SERVER_URL = "https://rasptempalarm.appspot.com"
SERVER_API = "temperature"
SERVER_API_VER = "v1"

MIN_PYTHON_VERSION = (2, 6)     # minimum supported python version

# Python version check
ver = sys.version_info
if (ver[0] == 3) or ((ver[0], ver[1]) < MIN_PYTHON_VERSION):
  print("error: Python version %s is unsupported. "
        "Please use Python 2.6 - 2.7 instead."
        % sys.version.split(' ')[0])
  sys.exit(1)

def main():
  print "Initializing server API..."

  http = httplib2.Http()
  try:
    service = build(SERVER_API, SERVER_API_VER, http=http,
                    discoveryServiceUrl=(SERVER_URL + "/_ah/api/discovery/v1/apis/" +
                                         SERVER_API + "/" + SERVER_API_VER + "/rest"))
    endpoint = service.temperatureEndpoint()
  except Exception as e:
    print "error:", e.__doc__, "Stopping..."
    sys.exit(1)

  print "Retrieve desired report rate from backend"
  reportRate = endpoint.getReportRate().execute()["value"]
  print "Backend requests report rate of", reportRate, "seconds"

  endpoint.report(temperature=32.1).execute()

  print "Stopping power alarm on server"
  endpoint.stop().execute()

  print "DONE"

if __name__ == "__main__":
  main()

