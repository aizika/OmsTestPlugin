package com.workday.plugin.omstest.remote

import javax.management.MBeanServerConnection
import javax.management.ObjectName
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import javax.management.JMX

object JmxConnectorUtil {
    fun connect(host: String, port: Int = 1099): JMXConnector {
        val url = JMXServiceURL("service:jmx:rmi:///jndi/rmi://$host:$port/jmxrmi")
        return JMXConnectorFactory.connect(url, null)
    }

    fun <T> JMXConnector.getMBeanProxy(name: String, clazz: Class<T>): T {
        val conn: MBeanServerConnection = this.mBeanServerConnection
        val objName = ObjectName(name)
        return JMX.newMBeanProxy(conn, objName, clazz)
    }
}