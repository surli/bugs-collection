var fluid_1_5=fluid_1_5||{},fluid=fluid||fluid_1_5;!function($,fluid){"use strict";fluid.thatistBridge=function(name,peer){var togo=function(funcname){for(var segs=funcname.split("."),move=peer,i=0;i<segs.length;++i)move=move[segs[i]];var args=[this];2===arguments.length&&(args=args.concat($.makeArray(arguments[1])));var ret=move.apply(null,args);this.that=function(){return ret};var type=typeof ret;return!ret||"string"===type||"number"===type||"boolean"===type||ret&&void 0!==ret.length?ret:this};return $.fn[name]=togo,togo},fluid.thatistBridge("fluid",fluid),fluid.thatistBridge("fluid_1_5",fluid_1_5);var normalizeTabindexName=function(){return $.browser.msie?"tabIndex":"tabindex"},canHaveDefaultTabindex=function(elements){return elements.length<=0?!1:$(elements[0]).is("a, input, button, select, area, textarea, object")},getValue=function(elements){if(elements.length<=0)return void 0;if(!fluid.tabindex.hasAttr(elements))return canHaveDefaultTabindex(elements)?Number(0):void 0;var value=elements.attr(normalizeTabindexName());return Number(value)},setValue=function(elements,toIndex){return elements.each(function(i,item){$(item).attr(normalizeTabindexName(),toIndex)})};fluid.tabindex=function(target,toIndex){return target=$(target),null!==toIndex&&void 0!==toIndex?setValue(target,toIndex):getValue(target)},fluid.tabindex.remove=function(target){return target=$(target),target.each(function(i,item){$(item).removeAttr(normalizeTabindexName())})},fluid.tabindex.hasAttr=function(target){if(target=$(target),target.length<=0)return!1;var togo=target.map(function(){var attributeNode=this.getAttributeNode(normalizeTabindexName());return attributeNode?attributeNode.specified:!1});return 1===togo.length?togo[0]:togo},fluid.tabindex.has=function(target){return target=$(target),fluid.tabindex.hasAttr(target)||canHaveDefaultTabindex(target)},fluid.a11y=$.a11y||{},fluid.a11y.orientation={HORIZONTAL:0,VERTICAL:1,BOTH:2};var UP_DOWN_KEYMAP={next:$.ui.keyCode.DOWN,previous:$.ui.keyCode.UP},LEFT_RIGHT_KEYMAP={next:$.ui.keyCode.RIGHT,previous:$.ui.keyCode.LEFT},unwrap=function(element){return element.jquery?element[0]:element},makeElementsTabFocussable=function(elements){elements.each(function(idx,item){item=$(item),(!item.fluid("tabindex.has")||item.fluid("tabindex")<0)&&item.fluid("tabindex",0)})};fluid.tabbable=function(target){target=$(target),makeElementsTabFocussable(target)};var CONTEXT_KEY="selectionContext",NO_SELECTION=-32768,cleanUpWhenLeavingContainer=function(selectionContext){selectionContext.activeItemIndex!==NO_SELECTION&&(selectionContext.options.onLeaveContainer?selectionContext.options.onLeaveContainer(selectionContext.selectables[selectionContext.activeItemIndex]):selectionContext.options.onUnselect&&selectionContext.options.onUnselect(selectionContext.selectables[selectionContext.activeItemIndex])),selectionContext.options.rememberSelectionState||(selectionContext.activeItemIndex=NO_SELECTION)},drawSelection=function(elementToSelect,handler){handler&&handler(elementToSelect)},eraseSelection=function(selectedElement,handler){handler&&selectedElement&&handler(selectedElement)},unselectElement=function(selectedElement,selectionContext){eraseSelection(selectedElement,selectionContext.options.onUnselect)},selectElement=function(elementToSelect,selectionContext){unselectElement(selectionContext.selectedElement(),selectionContext),elementToSelect=unwrap(elementToSelect);var newIndex=selectionContext.selectables.index(elementToSelect);-1!==newIndex&&(selectionContext.activeItemIndex=newIndex,drawSelection(elementToSelect,selectionContext.options.onSelect))},selectableFocusHandler=function(selectionContext){return function(evt){return $(evt.target).fluid("tabindex",0),selectElement(evt.target,selectionContext),evt.stopPropagation()}},selectableBlurHandler=function(selectionContext){return function(evt){return $(evt.target).fluid("tabindex",selectionContext.options.selectablesTabindex),unselectElement(evt.target,selectionContext),evt.stopPropagation()}},reifyIndex=function(sc_that){var elements=sc_that.selectables;sc_that.activeItemIndex>=elements.length&&(sc_that.activeItemIndex=sc_that.options.noWrap?elements.length-1:0),sc_that.activeItemIndex<0&&sc_that.activeItemIndex!==NO_SELECTION&&(sc_that.activeItemIndex=sc_that.options.noWrap?0:elements.length-1),sc_that.activeItemIndex>=0&&fluid.focus(elements[sc_that.activeItemIndex])},prepareShift=function(selectionContext){var selElm=selectionContext.selectedElement();selElm&&fluid.blur(selElm),unselectElement(selectionContext.selectedElement(),selectionContext),selectionContext.activeItemIndex===NO_SELECTION&&(selectionContext.activeItemIndex=-1)},focusNextElement=function(selectionContext){prepareShift(selectionContext),++selectionContext.activeItemIndex,reifyIndex(selectionContext)},focusPreviousElement=function(selectionContext){prepareShift(selectionContext),--selectionContext.activeItemIndex,reifyIndex(selectionContext)},arrowKeyHandler=function(selectionContext,keyMap){return function(evt){evt.which===keyMap.next?(focusNextElement(selectionContext),evt.preventDefault()):evt.which===keyMap.previous&&(focusPreviousElement(selectionContext),evt.preventDefault())}},getKeyMapForDirection=function(direction){var keyMap;return direction===fluid.a11y.orientation.HORIZONTAL?keyMap=LEFT_RIGHT_KEYMAP:direction===fluid.a11y.orientation.VERTICAL&&(keyMap=UP_DOWN_KEYMAP),keyMap},tabKeyHandler=function(selectionContext){return function(evt){evt.which===$.ui.keyCode.TAB&&(cleanUpWhenLeavingContainer(selectionContext),evt.shiftKey&&(selectionContext.focusIsLeavingContainer=!0))}},containerFocusHandler=function(selectionContext){return function(evt){var shouldOrig=selectionContext.options.autoSelectFirstItem,shouldSelect="function"==typeof shouldOrig?shouldOrig():shouldOrig;return selectionContext.focusIsLeavingContainer&&(shouldSelect=!1),shouldSelect&&evt.target===selectionContext.container.get(0)&&(selectionContext.activeItemIndex===NO_SELECTION&&(selectionContext.activeItemIndex=0),fluid.focus(selectionContext.selectables[selectionContext.activeItemIndex])),evt.stopPropagation()}},containerBlurHandler=function(selectionContext){return function(evt){return selectionContext.focusIsLeavingContainer=!1,evt.stopPropagation()}},makeElementsSelectable=function(container,defaults,userOptions){var options=$.extend(!0,{},defaults,userOptions),keyMap=getKeyMapForDirection(options.direction),selectableElements=options.selectableElements?options.selectableElements:container.find(options.selectableSelector),that={container:container,activeItemIndex:NO_SELECTION,selectables:selectableElements,focusIsLeavingContainer:!1,options:options};return that.selectablesUpdated=function(focusedItem){"number"==typeof that.options.selectablesTabindex&&that.selectables.fluid("tabindex",that.options.selectablesTabindex),that.selectables.unbind("focus."+CONTEXT_KEY),that.selectables.unbind("blur."+CONTEXT_KEY),that.selectables.bind("focus."+CONTEXT_KEY,selectableFocusHandler(that)),that.selectables.bind("blur."+CONTEXT_KEY,selectableBlurHandler(that)),keyMap&&that.options.noBubbleListeners&&(that.selectables.unbind("keydown."+CONTEXT_KEY),that.selectables.bind("keydown."+CONTEXT_KEY,arrowKeyHandler(that,keyMap))),focusedItem?selectElement(focusedItem,that):reifyIndex(that)},that.refresh=function(){that.options.selectableSelector||fluid.fail("Cannot refresh selectable context which was not initialised by a selector"),that.selectables=container.find(options.selectableSelector),that.selectablesUpdated()},that.selectedElement=function(){return that.activeItemIndex<0?null:that.selectables[that.activeItemIndex]},keyMap&&!that.options.noBubbleListeners&&container.keydown(arrowKeyHandler(that,keyMap)),container.keydown(tabKeyHandler(that)),container.focus(containerFocusHandler(that)),container.blur(containerBlurHandler(that)),that.selectablesUpdated(),that};fluid.selectable=function(target,options){target=$(target);var that=makeElementsSelectable(target,fluid.selectable.defaults,options);return fluid.setScopedData(target,CONTEXT_KEY,that),that},fluid.selectable.select=function(target,toSelect){fluid.focus(toSelect)},fluid.selectable.selectNext=function(target){target=$(target),focusNextElement(fluid.getScopedData(target,CONTEXT_KEY))},fluid.selectable.selectPrevious=function(target){target=$(target),focusPreviousElement(fluid.getScopedData(target,CONTEXT_KEY))},fluid.selectable.currentSelection=function(target){target=$(target);var that=fluid.getScopedData(target,CONTEXT_KEY);return $(that.selectedElement())},fluid.selectable.defaults={direction:fluid.a11y.orientation.VERTICAL,selectablesTabindex:-1,autoSelectFirstItem:!0,rememberSelectionState:!0,selectableSelector:".selectable",selectableElements:null,onSelect:null,onUnselect:null,onLeaveContainer:null,noWrap:!1};var checkForModifier=function(binding,evt){if(!binding.modifier)return!0;var modifierKey=binding.modifier,isCtrlKeyPresent=modifierKey&&evt.ctrlKey,isAltKeyPresent=modifierKey&&evt.altKey,isShiftKeyPresent=modifierKey&&evt.shiftKey;return isCtrlKeyPresent||isAltKeyPresent||isShiftKeyPresent},makeActivationHandler=function(binding){return function(evt){var target=evt.target;if(fluid.enabled(target)){var code=evt.which?evt.which:evt.keyCode;if(code===binding.key&&binding.activateHandler&&checkForModifier(binding,evt)){var event=$.Event("fluid-activate");$(target).trigger(event,[binding.activateHandler]),event.isDefaultPrevented()&&evt.preventDefault()}}}},makeElementsActivatable=function(elements,onActivateHandler,defaultKeys,options){var bindings=[];$(defaultKeys).each(function(index,key){bindings.push({modifier:null,key:key,activateHandler:onActivateHandler})}),options&&options.additionalBindings&&(bindings=bindings.concat(options.additionalBindings)),fluid.initEnablement(elements);for(var i=0;i<bindings.length;++i){var binding=bindings[i];elements.keydown(makeActivationHandler(binding))}elements.bind("fluid-activate",function(evt,handler){return handler=handler||onActivateHandler,handler?handler(evt):null})};fluid.activatable=function(target,fn,options){target=$(target),makeElementsActivatable(target,fn,fluid.activatable.defaults.keys,options)},fluid.activate=function(target){$(target).trigger("fluid-activate")},fluid.activatable.defaults={keys:[$.ui.keyCode.ENTER,$.ui.keyCode.SPACE]}}(jQuery,fluid_1_5);