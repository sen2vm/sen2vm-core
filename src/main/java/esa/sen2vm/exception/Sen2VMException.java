/** Copyright 2024-2025, CS GROUP, https://www.cs-soprasteria.com/
*
* This file is part of the Sen2VM Core project
*     https://gitlab.acri-cwa.fr/opt-mpc/s2_tools/sen2vm/sen2vm-core
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.*/

package esa.sen2vm.exception;

/**
 * Sen2VMException
 */
public class Sen2VMException extends Exception
{
    private static final long serialVersionUID = 1L;

    public Sen2VMException()
    {
        super();
    }

    /**
     * Constructor with exception cause
     * @param e exception cause
     */
    public Sen2VMException(Exception e)
    {
        super(e.getLocalizedMessage(), e);
    }

    /**
     * Constructor with message
     * @param msg the error message
     */
    public Sen2VMException(String msg)
    {
        super(msg);
    }

    /**
     * Constructor with message and exception cause
     * @param msg the error message
     * @param e exception cause
     */
    public Sen2VMException(String msg, Exception e)
    {
        super(msg, e);
    }
}