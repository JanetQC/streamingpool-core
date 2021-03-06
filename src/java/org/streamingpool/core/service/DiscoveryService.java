// @formatter:off
/**
*
* This file is part of streaming pool (http://www.streamingpool.org).
* 
* Copyright (c) 2017-present, CERN. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* 
*/
// @formatter:on

package org.streamingpool.core.service;

import org.reactivestreams.Publisher;

/**
 * Interface used to discover {@link Publisher}.
 * 
 * @see ProvidingService
 */
@FunctionalInterface
public interface DiscoveryService {

    /**
     * Given a {@link StreamId}, this method returns the correspondent {@link Publisher}. This method should not
     * return null, instead is preferred to throw a specific exception in the case the given id is not present in the
     * system. From the API level, this behavior is not forced.
     * 
     * @param id the identifier of the stream to be discovered
     * @return the discovered {@link Publisher}
     */
    <T> Publisher<T> discover(StreamId<T> id);

}
