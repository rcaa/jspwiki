/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */

package org.apache.wiki.util;

import java.net.ConnectException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.net.ssl.SSLHandshakeException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.PropertyConfigurator;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.util.MailUtil;

/**
 * This test is not integrated into any TestSuite yet, because I don't know how
 * to really assert if a mail was sent etc. (setup would be to complicated yet). Therefore
 * replace the static TEST variables with your configuration and test by hand: execute
 * testSendMail and see if you get the mail at the adress you indicate in "TEST_RECEIVER".
 * 
 *
 */
public class MailUtilTest extends TestCase
{
    Properties m_props = TestEngine.getTestProperties();

    WikiContext     m_context;

    static final String PAGE_NAME = "TestPage";
    
    static final String TEST_HOST = "mail.mydomain.org";
    static final String TEST_PORT = "587";
    static final String TEST_ACCOUNT = "myaccount";
    static final String TEST_PASSWORD = "changeit";
    
    static final String TEST_RECEIVER = "someone@somewhere.org";
    static final String TEST_SENDER = "homer.simpson@burns.com";
    
    public MailUtilTest( String s ) {
        super( s );
    }

    public void setUp() throws Exception {
        PropertyConfigurator.configure(m_props);
        TestEngine testEngine = new TestEngine( m_props );
        m_context = new WikiContext( testEngine,
                                     new WikiPage( testEngine, PAGE_NAME ) );
    }

    /**
     * Verifies that the properties loaded by tests/etc/jspwiki.properties are the ones we expect.
     * Three of them (account, password, jndi name) are commented out, so we expect null.
     */
    public void testProperties() 
    {
        Properties props = m_context.getEngine().getWikiProperties();
        assertEquals( "127.0.0.1",                   props.getProperty( MailUtil.PROP_MAIL_HOST ) );
        assertEquals( "25",                          props.getProperty( MailUtil.PROP_MAIL_PORT ) );
        assertEquals( "JSPWiki <JSPWiki@localhost>", props.getProperty( MailUtil.PROP_MAIL_SENDER ) );
        assertNull( props.getProperty( MailUtil.PROP_MAIL_ACCOUNT ) );
        assertNull( props.getProperty( MailUtil.PROP_MAIL_PASSWORD ) );
        assertNull( props.getProperty( MailUtil.PROP_MAIL_JNDI_NAME ) );
    }
    
    /**
     * This test sends a message to the local user's mailbox on this host. It assumes that
     * there is a locally-listening SMTP server on port 25, and that the current runtime
     * user has a mailbox on the local machine. For Unix-based systems such as Linux and
     * OS X, you can verify that this test ran successfully simply by typing "mail" at
     * the command line.
     */
    public void testSendMail()
    {
        m_props.setProperty( "jspwiki.usePageCache", "true" );

        String user = System.getProperty( "user.name" ) + "@localhost";
        
        try
        {
            MailUtil.sendMessage(m_context.getEngine().getWikiProperties(), 
                                 user, 
                                 "Mail test", 
                                 "This is a test mail generated by MailUtilTest.");            
        }
        catch( MessagingException e )
        {
            if( e.getCause() instanceof ConnectException )
            {
                // This can occur if you do not have a SMTP server set up.  We just log this and don't fail.
                System.out.println("I could not test whether mail sending works, since I could not connect to your SMTP server.");
                System.out.println("Reason: "+e.getMessage());
                return;
            }
            if( e.getCause() instanceof SSLHandshakeException )
            {
                // This can occur if you do not have the required cert in the JVM's keystore.  We just log this
                // and don't fail.
                System.out.println("I could not test whether mail sending works, since I don't have the required cert in my keystore.");
                System.out.println("Reason: "+e.getMessage());
                return;
            }
            e.printStackTrace();
            fail( "Unknown problem, cause=" + e.getCause() + " (check the console for error report)" );
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail( "Could not send mail: " + e.getMessage() );
        }
    }

    public static Test suite()
    {
        return new TestSuite( MailUtilTest.class );
    }

}
