/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.widgets.tuning;

import java.io.IOException;

import org.mavlink.messages.MAV_PARAM_TYPE;
import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_param_set;

import com.comino.flight.parameter.PX4Parameters;
import com.comino.flight.parameter.ParamUtils;
import com.comino.flight.parameter.ParameterAttributes;
import com.comino.flight.widgets.FadePane;
import com.comino.mav.control.IMAVController;
import com.comino.msp.log.MSPLogger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

public class TuningWidget extends FadePane  {


	@FXML
	private GridPane grid;

	@FXML
	private ChoiceBox<String> groups;

	private IMAVController control;
	private PX4Parameters  params;



	public TuningWidget() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TuningWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}

		params = PX4Parameters.getInstance();

		groups.getItems().add("None");

		params.getAttributeProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
				Platform.runLater(() -> {
					ParameterAttributes p = (ParameterAttributes)newValue;
					if(!groups.getItems().contains(p.group_name))
						groups.getItems().add(p.group_name);
					groups.getSelectionModel().clearAndSelect(0);
				});
			}
		});

	}

	@FXML
	public void initialize() {

		grid.setVgap(4); grid.setHgap(6);

		groups.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				grid.getChildren().clear();
				int i = 0;
				for(ParameterAttributes p : params.getList()) {
					if(newValue.contains(p.group_name)) {
						Label unit = new Label(p.unit); unit.setPrefWidth(30);
						Label name = new Label(p.name); name.setPrefWidth(100); name.setTooltip(new Tooltip(p.description));
						grid.addRow(i++, name,createParamControl(p),unit);
					}
				}
			}
		});

	}

	private Control createParamControl(ParameterAttributes p) {
		ParamItem item = new ParamItem(p);
		return item.editor;
	}


	public void setup(IMAVController control) {
		this.control = control;

	}

	private class ParamItem {

		public Control editor = null;
		private ParameterAttributes att = null;

		public ParamItem(ParameterAttributes att) {

			this.att= att;

			if(att.increment != 0) {
				if(att.vtype==MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32) {
					this.editor = new Spinner<Integer>(att.min_val, att.max_val, att.value,1);
				} else {
					//				float increment = att.increment != 0 ? att.increment : (float)Math.pow(10, -att.decimals);
					this.editor = new Spinner<Double>(att.min_val, att.max_val, 0 ,att.increment);
				}
			} else {
				this.editor = new TextField();
			}

			setValueOf(editor,att.value);


			this.editor.setPrefWidth(80);
			this.editor.setPrefHeight(19);
			this.editor.setStyle("-fx-control-inner-background: #202020;");

			this.editor.setOnKeyPressed(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent keyEvent)
				{
					if(keyEvent.getCode() == KeyCode.ENTER)
						editor.getParent().getParent().requestFocus();
					if(keyEvent.getCode() == KeyCode.ESCAPE) {
						setValueOf(editor,att.value);
						editor.getParent().getParent().requestFocus();
					}
				}
			});

			this.editor.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					if(!newValue.booleanValue()) {
						try {

							float val =  getValueOf(editor);
							if(val!=att.value) {

								if((val >= att.min_val && val <= att.max_val) ||
										att.min_val == att.max_val ) {

									msg_param_set msg = new msg_param_set(255,1);
									msg.target_component = 1;
									msg.target_system = 1;
									msg.param_type = att.vtype;
									msg.setParam_id(att.name);
									msg.param_value = ParamUtils.valToParam(att.vtype, val);

									control.sendMAVLinkMessage(msg);
									MSPLogger.getInstance().writeLocalMsg(att.name+" is updated on device",MAV_SEVERITY.MAV_SEVERITY_DEBUG);

									System.out.println("Value changed: "+val+" prev: "+ParamUtils.paramToVal(att.vtype,att.value));

								}
								else {
									MSPLogger.getInstance().writeLocalMsg(att.name+" is out of bounds ("+att.min_val+","+att.max_val+")",MAV_SEVERITY.MAV_SEVERITY_DEBUG);
									setValueOf(editor,att.value);
								}
							}
						} catch(NumberFormatException e) {
							setValueOf(editor,att.value);
						}
					}
				}

			});
		}


		@SuppressWarnings("unchecked")
		private float getValueOf(Control p) throws NumberFormatException {
			if(p instanceof TextField) {
				((TextField)p).commitValue();
				return Float.parseFloat(((TextField)p).getText());
			}
			else
				return (((Spinner<Double>)editor).getValueFactory().getValue()).floatValue();
		}

		@SuppressWarnings("unchecked")
		private void setValueOf(Control p, float v) {
			if(p instanceof TextField) {
				if(att.vtype==MAV_PARAM_TYPE.MAV_PARAM_TYPE_INT32)
					((TextField)p).setText(String.valueOf((int)v));
				else
					((TextField)p).setText(String.valueOf(v));
			}
			else
				((Spinner<Double>)p).getValueFactory().setValue(new Double(v));
		}

		//		private void setContextMenue(Control editor) {
		//					ContextMenu ctxm = new ContextMenu();
		//					MenuItem cmItem1 = new MenuItem("Set default");
		//					cmItem1.setOnAction(new EventHandler<ActionEvent>() {
		//						public void handle(ActionEvent e) {
		//							textField.setText(getStringOfDefault());
		//						}
		//					});
		//
		//					MenuItem cmItem2 = new MenuItem("Reset to previous");
		//					cmItem2.setOnAction(new EventHandler<ActionEvent>() {
		//						public void handle(ActionEvent e) {
		//							textField.setText(getStringOfOld());
		//						}
		//					});
		//		}
	}


}
