$wnd.org_vaadin_aceeditor_AceEditorDemoWidgetSet.runAsyncCallback5("defineClass(1840, 1, $intern_128);\n_.load_7 = function load_9(){\n  this.val$store2.setSuperClass(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerState_2_classLit, cggl.Lcom_vaadin_shared_AbstractComponentState_2_classLit);\n  this.val$store2.setClass('com.vaadin.ui.ColorPickerArea', cggl.Lcom_vaadin_client_ui_colorpicker_ColorPickerAreaConnector_2_classLit);\n  this.val$store2.setConstructor(cggl.Lcom_vaadin_client_ui_colorpicker_ColorPickerAreaConnector_2_classLit, new cvcm.ConnectorBundleLoaderImpl$5$1$1(this));\n  this.val$store2.setConstructor(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerState_2_classLit, new cvcm.ConnectorBundleLoaderImpl$5$1$2(this));\n  this.val$store2.setReturnType(cggl.Lcom_vaadin_client_ui_colorpicker_ColorPickerAreaConnector_2_classLit, 'getWidget', new cvcm.Type(cggl.Lcom_vaadin_client_ui_VColorPickerArea_2_classLit));\n  this.val$store2.setReturnType(cggl.Lcom_vaadin_client_ui_colorpicker_ColorPickerAreaConnector_2_classLit, 'getState', new cvcm.Type(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerState_2_classLit));\n  this.val$store2.setInvoker(cggl.Lcom_vaadin_client_ui_VColorPickerArea_2_classLit, 'setColor', new cvcm.ConnectorBundleLoaderImpl$5$1$3(this));\n  this.val$store2.setInvoker(cggl.Lcom_vaadin_client_ui_VColorPickerArea_2_classLit, 'setOpen', new cvcm.ConnectorBundleLoaderImpl$5$1$4(this));\n  this.loadJsBundle_3(this.val$store2);\n  this.val$store2.setPropertyType(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerState_2_classLit, 'color', new cvcm.Type(cggl.Ljava_lang_String_2_classLit));\n  this.val$store2.setPropertyType(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerState_2_classLit, 'popupVisible', new cvcm.Type(cggl.Ljava_lang_Boolean_2_classLit));\n  this.val$store2.setPropertyType(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerState_2_classLit, 'showDefaultCaption', new cvcm.Type(cggl.Ljava_lang_Boolean_2_classLit));\n  this.val$store2.setDelegateToWidget(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerState_2_classLit, 'color', 'setColor');\n  this.val$store2.setDelegateToWidget(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerState_2_classLit, 'popupVisible', 'setOpen');\n  this.val$store2.addOnStateChangeMethod(cggl.Lcom_vaadin_client_ui_colorpicker_ColorPickerAreaConnector_2_classLit, new cvcm.OnStateChangeMethod(cggl.Lcom_vaadin_client_ui_AbstractComponentConnector_2_classLit, 'handleContextClickListenerChange', initValues(getClassLiteralForArray(cggl.Ljava_lang_String_2_classLit, 1), $intern_2, 2, 4, ['registeredEventListeners'])));\n}\n;\n_.loadJsBundle_3 = function loadJsBundle_3(store){\n  this.loadNativeJs_3(store);\n}\n;\n_.loadNativeJs_3 = function loadNativeJs_3(store){\n  var data_0 = {setter:function(bean, value_0){\n    bean.color_0 = value_0;\n  }\n  , getter:function(bean){\n    return bean.color_0;\n  }\n  };\n  store.setPropertyData(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerState_2_classLit, 'color', data_0);\n  var data_0 = {setter:function(bean, value_0){\n    bean.popupVisible = value_0.booleanValue();\n  }\n  , getter:function(bean){\n    return jl.valueOf_70(bean.popupVisible);\n  }\n  };\n  store.setPropertyData(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerState_2_classLit, 'popupVisible', data_0);\n  var data_0 = {setter:function(bean, value_0){\n    bean.showDefaultCaption = value_0.booleanValue();\n  }\n  , getter:function(bean){\n    return jl.valueOf_70(bean.showDefaultCaption);\n  }\n  };\n  store.setPropertyData(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerState_2_classLit, 'showDefaultCaption', data_0);\n}\n;\n_.onSuccess_1 = function onSuccess_9(){\n  this.load_7();\n  cvcm.get_30().setLoaded_0(this.this$11.getName());\n}\n;\ndefineClass(1842, 1, $intern_173, cvcm.ConnectorBundleLoaderImpl$5$1$1);\n_.$init_1241 = function $init_1241(){\n}\n;\n_.invoke_0 = function invoke_290(target, params){\n  return new cvcuc3.ColorPickerAreaConnector;\n}\n;\ndefineClass(1843, 1, $intern_173, cvcm.ConnectorBundleLoaderImpl$5$1$2);\n_.$init_1242 = function $init_1242(){\n}\n;\n_.invoke_0 = function invoke_291(target, params){\n  return new cvsuc3.ColorPickerState;\n}\n;\ndefineClass(1844, 1, $intern_173, cvcm.ConnectorBundleLoaderImpl$5$1$3);\n_.$init_1243 = function $init_1243(){\n}\n;\n_.invoke_0 = function invoke_292(target, params){\n  dynamicCast(target, 421).setColor(dynamicCastToString(params[0]));\n  return null;\n}\n;\ndefineClass(1845, 1, $intern_173, cvcm.ConnectorBundleLoaderImpl$5$1$4);\n_.$init_1244 = function $init_1244(){\n}\n;\n_.invoke_0 = function invoke_293(target, params){\n  dynamicCast(target, 421).setOpen(dynamicCast(params[0], 52).booleanValue());\n  return null;\n}\n;\ndefineClass(421, 6, $intern_297, cvcu.VColorPickerArea);\n_.$init_1347 = function $init_1347(){\n  this.color_0 = null;\n}\n;\n_.addClickHandler = function addClickHandler_7(handler){\n  return this.addDomHandler(handler, cggedc.getType_10());\n}\n;\n_.isOpen_0 = function isOpen_2(){\n  return this.isOpen;\n}\n;\n_.onBrowserEvent_0 = function onBrowserEvent_15(event_0){\n  var type_0;\n  type_0 = cgguc.eventGetType_0(event_0);\n  switch (type_0) {\n    case 1:\n      if (cgguc.isOrHasChild_3(this.area.getElement(), cgguc.eventGetTarget_2(event_0))) {\n        getClassPrototype(6).onBrowserEvent_0.call(this, event_0);\n      }\n\n      break;\n    default:getClassPrototype(6).onBrowserEvent_0.call(this, event_0);\n  }\n}\n;\n_.onClick = function onClick_38(event_0){\n  this.setOpen(!this.isOpen);\n}\n;\n_.refreshColor = function refreshColor_0(){\n  if (jsNotEquals(this.color_0, null)) {\n    cggdc.$setProperty_0(cggdc.$getStyle(this.area.getElement()), 'background', this.color_0);\n  }\n}\n;\n_.setColor = function setColor_0(color_0){\n  this.color_0 = color_0;\n}\n;\n_.setHTML = function setHTML_9(html){\n  this.caption_0.setHTML(html);\n}\n;\n_.setHeight_0 = function setHeight_5(height){\n  this.area.setHeight_0(height);\n}\n;\n_.setOpen = function setOpen_0(open_0){\n  this.isOpen = open_0;\n}\n;\n_.setStylePrimaryName = function setStylePrimaryName_5(style){\n  getClassPrototype(11).setStylePrimaryName.call(this, style);\n  this.area.setStylePrimaryName(this.getStylePrimaryName() + '-area');\n}\n;\n_.setText = function setText_12(text_0){\n  this.caption_0.setText(text_0);\n}\n;\n_.setWidth_0 = function setWidth_6(width_0){\n  this.area.setWidth_0(width_0);\n}\n;\ndefineClass(1841, 823, $intern_296, cvcuc3.ColorPickerAreaConnector);\n_.$init_1614 = function $init_1614(){\n  this.rpc = dynamicCast(cvcc.create_15(cggl.Lcom_vaadin_shared_ui_colorpicker_ColorPickerServerRpc_2_classLit, this), 683);\n}\n;\n_.createWidget = function createWidget_7(){\n  return dynamicCast(new cvcu.VColorPickerArea, 6);\n}\n;\n_.getWidget_0 = function getWidget_32(){\n  return this.getWidget_15();\n}\n;\n_.getWidget_15 = function getWidget_33(){\n  return dynamicCast(getClassPrototype(13).getWidget_0.call(this), 421);\n}\n;\n_.onClick = function onClick_55(event_0){\n  this.rpc.openPopup_0(this.getWidget_15().isOpen_0());\n}\n;\n_.refreshColor = function refreshColor_1(){\n  this.getWidget_15().refreshColor();\n}\n;\n_.setCaption = function setCaption_10(caption){\n  cvc.setCaptionText_0(this.getWidget_15(), this.getState_21());\n}\n;\n$entry(onLoad)(5);\n\n//# sourceURL=org.vaadin.aceeditor.AceEditorDemoWidgetSet-5.js\n")
