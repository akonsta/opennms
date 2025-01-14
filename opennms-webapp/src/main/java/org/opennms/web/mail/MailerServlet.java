/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opennms.core.utils.StreamUtils;
import org.opennms.web.servlet.MissingParameterException;

/**
 * <p>MailerServlet class.</p>
 *
 * @author <A HREF="mailto:jacinta@opennms.org">Jacinta Remedios </A>
 * @author <A HREF="mailto:larry@opennms.org">Lawrence Karnowski </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 */
public class MailerServlet extends HttpServlet {
    /**
     * 
     */
    private static final Logger LOG = LoggerFactory.getLogger(MailerServlet.class);
    private static final long serialVersionUID = -6241742874510146572L;

    /** Constant <code>REQUIRED_FIELDS="new String[] { sendto, subject, usernam"{trunked}</code> */
    protected static final String[] REQUIRED_FIELDS = new String[] { "sendto", "subject", "username", "msg" };

    protected String redirectSuccess;

    protected String mailProgram;

    protected EmailValidator emailValidator = EmailValidator.getInstance();

    String[] invalidSubstr = new String[]{
            ";",
            "\\",
            "..",
            "&",
            "*",
            "'",
            "\"",
            "$",
            "%"
    };

    /**
     * <p>init</p>
     *
     * @throws javax.servlet.ServletException if any.
     */
    @Override
    public void init() throws ServletException {
        ServletConfig config = this.getServletConfig();

        this.redirectSuccess = config.getInitParameter("redirect.success");
        this.mailProgram = config.getInitParameter("mail.program");

        if (this.redirectSuccess == null) {
            throw new ServletException("Missing required init parameter: redirect.success");
        }

        if (this.mailProgram == null) {
            throw new ServletException("Missing required init parameter: mail.program");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sendto = request.getParameter("sendto");
        String subject = request.getParameter("subject");
        String msg = request.getParameter("msg");
        String username = request.getRemoteUser();

        LOG.debug("To: {}, Subject: {}, message: {}, username: {}", sendto, subject, msg, username);

        if (sendto == null) {
            throw new MissingParameterException("sendto", REQUIRED_FIELDS);
        }
        else if (!emailValidator.isValid(sendto)) {
            throw new IllegalArgumentException("sendto is an invalid email address.");
        }

        if (subject == null) {
            throw new MissingParameterException("subject", REQUIRED_FIELDS);
        }
        else {
            subject = validate(subject);
        }

        // Written to mail stdin, don't validate?
        if (msg == null) {
            throw new MissingParameterException("msg", REQUIRED_FIELDS);
        }

        if (username == null) {
            username = "";
        }

        String[] cmdArgs = { this.mailProgram, "-s", subject, sendto };
        Process process = Runtime.getRuntime().exec(cmdArgs);

        // send the message to the stdin of the mail command
        PrintWriter stdinWriter = new PrintWriter(process.getOutputStream());
        stdinWriter.print(msg);
        stdinWriter.close();

        // get the stderr to see if the command failed
        BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        if (err.ready()) {
            // get the error message
            StringWriter tempErr = new StringWriter();
            StreamUtils.streamToStream(err, tempErr);
            String errorMessage = tempErr.toString();

            // log the error message
            LOG.warn("Read from stderr: {}", errorMessage);

            // send the error message to the client
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            StreamUtils.streamToStream(new StringReader(errorMessage), out);
            out.close();
        } else {
            response.sendRedirect(this.redirectSuccess);
        }
    }

    /**
     *  Basic attempt at string sanitization
     * @param str   String to 'sanitize'
     * @return      'Sanitized' string
     */
    private String validate(String str) {
        for (String invalid : invalidSubstr) {
            if (str.contains(invalid)) {
                str = str.replace(invalid, "");
            }
        }
        return str;
    }
}
