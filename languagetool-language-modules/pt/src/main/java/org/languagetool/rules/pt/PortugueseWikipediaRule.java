/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005-2015 Daniel Naber (http://www.danielnaber.de)
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
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules.pt;

import org.languagetool.language.Portuguese;
import org.languagetool.rules.AbstractSimpleReplaceRule2;
import org.languagetool.rules.Categories;
import org.languagetool.rules.Example;
import org.languagetool.rules.ITSIssueType;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A rule that matches words which should not be used and suggests correct ones instead. 
 * Romanian implementations. Loads the list of words from
 * <code>/ro/replace.txt</code>.
 *
 * @author Tiago F. Santos 
 * @since 3.6
 */
public class PortugueseWikipediaRule extends AbstractSimpleReplaceRule2 {

  public static final String WIKIPEDIA_COMMON_ERRORS = "WIKIPEDIA_COMMON_ERRORS";

  private static final String FILE_NAME = "/pt/wikipedia.txt";
  private static final Locale PT_LOCALE = new Locale("pt");// locale used on case-conversion

  @Override
  public final String getFileName() {
    return FILE_NAME;
  }


  public PortugueseWikipediaRule(ResourceBundle messages) throws IOException {
    super(messages, new Portuguese());
    super.setCategory(Categories.WIKIPEDIA.getCategory(messages));
    setLocQualityIssueType(ITSIssueType.Grammar);
    addExamplePair(Example.wrong("<marker>mais também</marker>"),
                   Example.fixed("<marker>mas também</marker>"));
  }

  @Override
  public final String getId() {
    return WIKIPEDIA_COMMON_ERRORS;
  }

  @Override
  public String getDescription() {
    return "Erros frequentes nos artigos da Wikipédia";
  }

  @Override
  public String getShort() {
    return "Erro gramatical";
  }
  
  @Override
  public String getSuggestion() {
    return " é um erro gramatical. Considere utilizar ";
  }

  @Override
  public String getSuggestionsSeparator() {
    return " ou ";
  }

  @Override
  public Locale getLocale() {
    return PT_LOCALE;
  }

}
