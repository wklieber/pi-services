/*
 * Copyright (c) 2015, Werner Klieber. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package pi;

import org.junit.Test;
import pi.tools.HttpTools;

/**
 * Created by wklieber on 01.03.2015.
 */
public class WklieberExperimental {
    public static void main(String[] args) throws Exception {
        WklieberExperimental test = new WklieberExperimental();
        test.testHelloWorld();
    }

    @Test
    public void testHelloWorld() throws Exception {
        String baseUrl;
        //baseUrl = "http://localhost:8080";
        baseUrl = "http://192.168.1.60:8080";

        String rest = baseUrl + "/hello-world";

        String text = HttpTools.makeHttpRequest(rest, null, HttpTools.FORMAT.JSON, HttpTools.FORMAT.JSON);
        System.out.println("-------------------------------------------");
        System.out.println(text);
        System.out.println("-------------------------------------------");
    }
}
