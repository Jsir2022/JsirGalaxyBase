/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Derived from GTNH ServerUtilities ClaimResult and extended for versioned personal ownership.
 */
package com.jsirgalaxybase.modules.land.domain;

public enum LandActionResult {
    SUCCESS,
    DIMENSION_BLOCKED,
    RESERVED,
    LIMIT_REACHED,
    ALREADY_CLAIMED,
    NOT_CLAIMED,
    NOT_OWNER,
    VERSION_CONFLICT,
    TITLE_LOCKED,
    SERVER_MISMATCH,
    REQUEST_CONFLICT,
    INVALID_REQUEST
}
