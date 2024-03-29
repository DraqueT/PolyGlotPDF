/*
* Copyright (c) 2014-2020, Draque Thompson, draquemail@gmail.com
 * All rights reserved.
 *
 * Licensed under: MIT Licence
 * See LICENSE.TXT included with this code to read the full license agreement.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
// This is the type which all nodes and storage types extend.
package PolyGlot.Nodes;

import java.util.Objects;
import PolyGlot.CustomControls.PAlphaMap;
import PolyGlot.ManagersCollections.DictionaryCollection;

/**
 *
 * @author draque
 */
public abstract class DictNode implements Comparable<DictNode> {
    protected String value;
    protected Integer id;    
    protected DictionaryCollection parent = null;

    @Override
    abstract public boolean equals(Object comp);

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.value);
        hash = 83 * hash + Objects.hashCode(this.id);
        return hash;
    }
    
    protected DictNode() {
        value = "";
        id = 0;
    }
    
    protected DictNode(int _id) {
        id = _id;
        value = "";
    }
    
    /**
     * Sets a node equal to the argument node
     *
     * @param _node Node to set all values equal to.
     */
    public abstract void setEqual(DictNode _node) throws ClassCastException;

    public void setId(Integer _id) {
        id = _id;
    }

    public Integer getId() {
        return id;
    }

    public void setParent(DictionaryCollection _parent) {
        parent = _parent;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String conWord) {
        this.value = conWord.trim();
    }

    /**
     * implements compareTo in way that custom alpha sorting may be used
     *
     * @param _compare value to compare to this one
     * @return
     */
    @Override
    public int compareTo(DictNode _compare) {
        PAlphaMap<String, Integer> alphaOrder = getAlphaOrder();
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;
        final String comp = _compare.getValue();
        final String me = this.getValue();
        int ret;
        
        // if no alpha order established whatsoever or if parent is missing characters, use default sort
        if (alphaOrder.isMissingChars() || alphaOrder.isEmpty()) {
            ret = me.compareTo(comp);
        } else {
            if (comp.equals(me) || (comp.isEmpty() && me.isEmpty())) {
                ret = EQUAL;
            } else if (comp.isEmpty()) {
                ret = AFTER;
            } else if (me.isEmpty()) {
                ret = BEFORE;
            } else {
                // compare values based on largest front facing clusters found in alphabet order
                int longest = alphaOrder.getLongestEntry();
                int meLen = me.length();
                int compLen = comp.length();
                int meAlpha = -1;
                int compAlpha = -1;
                int preLen = 0;

                for (int i = Math.min(meLen, longest); i >= 0; i--) {
                    String mePrefix = me.substring(0, i);

                    if (alphaOrder.containsKey(mePrefix)) {
                        meAlpha = alphaOrder.get(mePrefix);
                        break;
                    }
                }
                
                for (int i = Math.min(compLen, longest); i >= 0; i--) {
                    String compPrefix = comp.substring(0, i);

                    if (alphaOrder.containsKey(compPrefix)) {
                        compAlpha = alphaOrder.get(compPrefix);
                        preLen = compPrefix.length(); // record length for substring truncation if current patterns are equal
                        break;
                    }
                }

                if (meAlpha == -1 && compAlpha == -1) {
                    // neither value is accounted for: equal
                    ret = EQUAL;
                } else if (meAlpha == -1) {
                    // no prefixed value for own value: default to after
                    ret = BEFORE;
                } else if (compAlpha == -1) {
                    // no prefixed pattern found for comp value: default placing comparison after
                    ret = AFTER;
                } else if (compAlpha > meAlpha) {
                    ret = BEFORE;
                } else if (compAlpha < meAlpha) {
                    ret = AFTER;
                } else {
                    // two patterns are the same. Truncate and check subpatterns
                    ConWord compChild = new ConWord();
                    ConWord thisChild = new ConWord();

                    compChild.setParent(parent);
                    thisChild.setParent(parent);

                    compChild.setValue(_compare.getValue().substring(preLen));
                    thisChild.setValue(this.getValue().substring(preLen));

                    ret = thisChild.compareTo(compChild);
                }
            }
        }

        return ret;
    }
    
    private PAlphaMap<String, Integer> getAlphaOrder() {
        PAlphaMap<String, Integer> ret;
        
        if (parent == null) {
            ret = new PAlphaMap<>();
        } else {
            ret = parent.getAlphaOrder();
        }
        
        return ret;
    }

    @Override
    public String toString() {
        return value.isEmpty() ? " " : value;
    }
}
