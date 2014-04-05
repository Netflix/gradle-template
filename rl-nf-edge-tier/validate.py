#!/usr/bin/env python

import argparse
import json
import requests
import time
import random


def validateTestEndpointImplementation(baseUrl):
    try:
        seed = random.randint(10000, 99999)
        url = baseUrl + "/test?id=" + str(seed)
        print "Validating WsPerfLab Implementation at URL: " + url
        print ""
        response = requests.get(url)
        
        #if(response.headers['server_response_time'] is None):
        #    raise Exception("Validation Failed! => missing 'server_response_time' response header.")
        #
        #if(response.headers['load_avg_per_core'] is None):
        #    raise Exception("Validation Failed! => missing 'load_avg_per_core' response header.")
        #
        #if(response.headers['os_arch'] is None):
        #    raise Exception("Validation Failed! => missing 'os_arch' response header.")
        #
        #if(response.headers['os_name'] is None):
        #    raise Exception("Validation Failed! => missing 'os_name' response header.")
        #
        #if(response.headers['os_version'] is None):
        #    raise Exception("Validation Failed! => missing 'os_version' response header.")
        
        # print data[0]
        jsonData = response.json()
        #print "ResponseKey: " + str(jsonData['responseKey'])
        #print "Expected: " + str(expectedResponseKeyForTestCaseA(seed))
        
        if jsonData['responseKey'] != expectedResponseKeyForTestCaseA(seed):
            raise Exception("Validation Failed! => ResponseKey Invalid.")
        
        # now check for JSON keys
        validateKey(jsonData, 'delay', 5)
        validateKey(jsonData, 'itemSize', 5)
        validateKey(jsonData, 'numItems', 5)
        validateKey(jsonData, 'items', 129)
        
        # if we didn't fail validation report success
        print "Successful Validation"    
        
        print ""
    except Exception as e:
        print "Error => " + str(e)
        print ""


def validateKey(jsonData, key, size):
    if key not in jsonData:
        raise Exception("Validation Failed! => Missing '" + key + "' key")
    if len(jsonData[key]) != size:
        raise Exception("Validation Failed! => '" + key + "' list size != " + str(size))

# Get the expected responseKey for TestCaseA
# for the given seed number
def expectedResponseKeyForTestCaseA(seed):
    requestResponseKey = ((seed / 37) + 5739375) * 7
    x = ((requestResponseKey / 37) + 5739375) * 7
    return x * 3


# main execution flow
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Validate a WSPerfLab implementation.')
    parser.add_argument('url', help='base url such as: http://localhost:8888/ws-java-servlet-blocking')
    args = parser.parse_args()
    
    validateTestEndpointImplementation(args.url)