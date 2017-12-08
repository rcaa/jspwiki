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
package org.apache.wiki.markdown.extensions.jspwikilinks.attributeprovider;

import org.apache.wiki.WikiContext;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.parser.MarkupParser;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.util.sequence.CharSubSequence;


/**
 * {@link NodeAttributeProviderState} which sets the attributes for local links.
 */
public class LocalLinkAttributeProviderState implements NodeAttributeProviderState< Link > {

    private final boolean hasRef;
    private final WikiContext wikiContext;
    private final LinkParsingOperations linkOperations;

    public LocalLinkAttributeProviderState( final WikiContext wikiContext, final boolean hasRef ) {
        this.hasRef = hasRef;
        this.wikiContext = wikiContext;
        this.linkOperations = new LinkParsingOperations( wikiContext );
    }

    /**
     * {@inheritDoc}
     *
     * @see NodeAttributeProviderState#setAttributes(Attributes, Link)
     */
    @Override
    public void setAttributes( final Attributes attributes, final Link link ) {
        final int hashMark = link.getUrl().toString().indexOf( '#' );
        final String attachment = wikiContext.getEngine().getAttachmentManager().getAttachmentInfoName( wikiContext, link.getUrl().toString() );
        if( attachment != null ) {
            if( !linkOperations.isImageLink( link.getUrl().toString() ) ) {
                attributes.replaceValue( "class", MarkupParser.CLASS_ATTACHMENT );
                final String attlink = wikiContext.getURL( WikiContext.ATTACH, link.getUrl().toString() );
                link.setUrl( CharSubSequence.of( attlink ) );
                attributes.replaceValue( "href", attlink );
            } else {
                new ImageLinkAttributeProviderState( wikiContext, attachment, hasRef ).setAttributes( attributes, link );
            }
        } else if( hashMark != -1 ) { // It's an internal Wiki link, but to a named section
            final String namedSection = link.getUrl().toString().substring( hashMark + 1 );
            link.setUrl( CharSubSequence.of( link.getUrl().toString().substring( 0, hashMark ) ) );
            final String matchedLink = linkOperations.linkIfExists( link.getUrl().toString() );
            if( matchedLink != null ) {
                String sectref = "#section-" + wikiContext.getEngine().encodeName( matchedLink + "-" + MarkupParser.wikifyLink( namedSection ) );
                sectref = sectref.replace('%', '_');
                new LocalReadLinkAttributeProviderState( wikiContext, link.getUrl().toString() + sectref ).setAttributes( attributes, link );
            } else {
                new LocalEditLinkAttributeProviderState( wikiContext, link.getUrl().toString() ).setAttributes( attributes, link );
            }
        } else {
            if( linkOperations.linkExists( link.getUrl().toString() ) ) {
                new LocalReadLinkAttributeProviderState( wikiContext, link.getUrl().toString() ).setAttributes( attributes, link );
            } else {
                new LocalEditLinkAttributeProviderState( wikiContext, link.getUrl().toString() ).setAttributes( attributes, link );
            }
        }
    }

}
