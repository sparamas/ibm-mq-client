/*
 * (c) Copyright IBM Corporation 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Author: sarav.paramasamy@ibm.com
 */

package com.ibm.jmsclient;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

import javax.jms.JMSException;

@ComponentScan(basePackages = "com.ibm.jmsclient")
public class AppConfig {

    private String ibmMqUserName=null;
    private String ibmMqPassword=null;
    private String ibmMqChannelName=null;
    private String ibmMqHostName=null;
    private String ibmQueueManager=null;
    private String ibmMqQueueName=null;
    private int ibmMqPort=1414;
    private String connectionType=null;

    public AppConfig(Settings mqSettings){
        this.ibmMqUserName=mqSettings.getIbmMqUserName();
        this.ibmMqPassword=mqSettings.getIbmMqPassword();
        this.ibmMqChannelName=mqSettings.getIbmMqChannelName();
        this.ibmMqHostName=mqSettings.getIbmMqHostName();
        this.ibmQueueManager=mqSettings.getIbmQueueManager();
        this.ibmMqQueueName=mqSettings.getIbmMqQueueName();
        this.ibmMqPort= Integer.parseInt(mqSettings.getIbmMqPort());
        this.connectionType=mqSettings.getConnectionType();
    }

    Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    @Autowired
    private Environment env;

    /**
     * Method provides setting up the MQConnectionFactory Bean in Client mode
     *
     * @return MQConnectionFactory is set up in client mode
     * @throws JMSException if setting up MQConnectionFactory failed
     * 	java -DUserName="app" -DPassword="passw0rd" -DQueueManager="QM1" -DHostName="localhost(1414)" -DChannelName="DEV.APP.SVRCONN" -DLoginPassword="hcsc" -jar target/spring-boot-web.jar
        System.setProperty("IBM_MQ_USER_NAME",(String)session.getAttribute("IBM_MQ_USER_NAME"));
        System.setProperty("IBM_MQ_PASSWORD", (String)session.getAttribute("IBM_MQ_PASSWORD"));
        System.setProperty("IBM_MQ_CHANNEL", (String)session.getAttribute("IBM_MQ_CHANNEL"));
        System.setProperty("IBM_HOST_NAME", (String)session.getAttribute("IBM_HOST_NAME"));
        System.setProperty("IBM_MQ_QM", (String)session.getAttribute("IBM_MQ_QM"));
        System.setProperty("IBM_MQ_PORT", (String)session.getAttribute("IBM_MQ_PORT"));
     */
    public MQConnectionFactory mqClientConnectionFactory() {
        MQConnectionFactory connectionFactory = new MQConnectionFactory();
        try {
            connectionFactory.setHostName(ibmMqHostName);
            connectionFactory.setPort(ibmMqPort);
            connectionFactory.setQueueManager(ibmQueueManager);
            connectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
          //  connectionFactory.setCCSID("");
          //  connectionFactory.setChannel("DEV.APP.SVRCONN");
            connectionFactory.setChannel(ibmMqChannelName);

            //Set up TlS connection
            if (connectionType.equalsIgnoreCase("SECURED")) {
                connectionFactory.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12");
            }

        } catch (JMSException je) {
            LOGGER.error("Cannot set up client connection factory" + je.getMessage());
        }
        return connectionFactory;
    }

    /**
     * Method provides setting up UserCredentialsConnectionFactoryAdapter Bean for using in Client mode
     *
     * @return UserCredentialsConnectionFactoryAdapter
     */
    public UserCredentialsConnectionFactoryAdapter jmsQueueConnectionFactorySecured() {
        UserCredentialsConnectionFactoryAdapter connectionFactoryAdapter = new UserCredentialsConnectionFactoryAdapter();
        connectionFactoryAdapter.setTargetConnectionFactory(mqClientConnectionFactory());
        connectionFactoryAdapter.setUsername(ibmMqUserName);
        connectionFactoryAdapter.setPassword(ibmMqPassword);
        return connectionFactoryAdapter;
    }

    /**
     * Method provides setting up JmsTemplate Bean
     *
     * @return DynamicDestinationResolver
     */
    public JmsTemplate jmsQueueTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(jmsQueueConnectionFactorySecured()); // optionally switching client mode
//        jmsTemplate.setConnectionFactory(jmsQueueConnectionFactory()); // optionally switching binding mode
        jmsTemplate.setDestinationResolver(destinationResolver());
        jmsTemplate.setReceiveTimeout(10000);
        return jmsTemplate;
    }

    /**
     * Method provides setting up DynamicDestinationResolver Bean
     *
     * @return DynamicDestinationResolver
     */
    public DynamicDestinationResolver destinationResolver() {
        return new DynamicDestinationResolver();
    }

    /**
     * Method provides setting up Single Connection Factory Bean for using in Binding mode
     *
     * @return SingleConnectionFactory
     */
/*    @Bean
    public SingleConnectionFactory jmsQueueConnectionFactory() {
        SingleConnectionFactory singleConnectionFactory = new SingleConnectionFactory();
        singleConnectionFactory.setTargetConnectionFactory(mqBindingConnectionFactory());
        singleConnectionFactory.setReconnectOnException(true);
        return singleConnectionFactory;
    }*/

}
