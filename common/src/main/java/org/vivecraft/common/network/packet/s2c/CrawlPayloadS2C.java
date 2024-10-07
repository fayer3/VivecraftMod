package org.vivecraft.common.network.packet.s2c;

import org.vivecraft.common.network.packet.PayloadIdentifier;

/**
 * indicates that the server supports roomscale crawling
 */
public record CrawlPayloadS2C() implements VivecraftPayloadS2C {

    @Override
    public PayloadIdentifier id() {
        return PayloadIdentifier.CRAWL;
    }
}