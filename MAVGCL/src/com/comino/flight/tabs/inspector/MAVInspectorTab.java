/*
 * Copyright (c) 2016 by E.Mansfeld
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
 */

package com.comino.flight.tabs.inspector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comino.mav.control.IMAVController;
import com.comino.mav.mavlink.IMAVLinkMsgListener;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class MAVInspectorTab extends BorderPane implements IMAVLinkMsgListener {

	@FXML
	private TreeTableView<Dataset> treetableview;

	@FXML
	private TreeTableColumn<Dataset, String> message_col;

	@FXML
	private TreeTableColumn<Dataset, String> variable_col;

	@FXML
	private TreeTableColumn<Dataset, Number>  value_col;

	@FXML
	private TextField t_filter;

	@FXML
	private Button b_clear;

	private String filter;

	final ObservableMap<String,Data> allData = FXCollections.observableHashMap();
	ObservableList<String> numberStrings = FXCollections.observableArrayList("Eins", "Zwei", "Drei", "Vier");

	public MAVInspectorTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MAVInspectorTab.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

	}

	@FXML
	private void initialize() {

		t_filter.setText("<filter>");
		t_filter.setStyle("-fx-text-inner-color: gray;");

		TreeItem<Dataset> root = new TreeItem<Dataset>(new Dataset("", 0));
		treetableview.setRoot(root);
		treetableview.setShowRoot(false);
		root.setExpanded(true);

		message_col.setCellValueFactory((param) -> {
			return param.getValue().isLeaf() ? new SimpleStringProperty("") : param.getValue().getValue().strProperty();
		});

		variable_col.setCellValueFactory(new Callback<CellDataFeatures<Dataset, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Dataset, String> param) {
				return param.getValue().isLeaf() ? param.getValue().getValue().strProperty() : new SimpleStringProperty("");
			}
		});

		value_col.setCellValueFactory(new Callback<CellDataFeatures<Dataset, Number>, ObservableValue<Number>>() {
			@Override
			public ObservableValue<Number> call(CellDataFeatures<Dataset, Number> param) {
				return param.getValue().getValue().noProperty();
			}
		});



		b_clear.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				enableFilter(false);
			}

		});

		t_filter.focusedProperty().addListener(new ChangeListener<Boolean>()
		{
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue)
			{
				if (newPropertyValue) {
					enableFilter(t_filter.getText().equals("<filter>"));
				} else {
					if(t_filter.getText().equals("")) {
						enableFilter(false);
					} else
						filter = t_filter.getText();
				}
			}
		});

		t_filter.textProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> arg0, String oldPropertyValue, String newPropertyValue)
			{
				if(!newPropertyValue.isEmpty() && !newPropertyValue.startsWith("<")) {
					filter = newPropertyValue;
				}

			}
		});


	}


	public MAVInspectorTab setup(IMAVController control) {
		control.addMAVLinkMsgListener(this);
		return this;
	}

	@Override
	public void received(Object _msg) {

		if(filter!=null) {
			if(_msg.toString().toLowerCase().contains(filter.toLowerCase()))
				parseMessageString(_msg.toString());
		}
		else
			parseMessageString(_msg.toString());
	}


	private void enableFilter(boolean enable) {
		if(enable) {
			t_filter.setText("");
			t_filter.setStyle("-fx-text-inner-color: black;");
		} else {
			filter = null;
			t_filter.setText("<filter>");
			t_filter.setStyle("-fx-text-inner-color: gray;");
		}
	}


	private void parseMessageString(String msg) {
		String _msg = msg.substring(0, 20);
		if(!allData.containsKey(_msg)) {

			ObservableMap<String,Dataset> variables =  FXCollections.observableHashMap();

			variables.put("tms", new Dataset("tms",System.currentTimeMillis()));
			Data data = new Data(_msg,variables);

			allData.put(_msg,data);

			TreeItem<Dataset> ti = new TreeItem<>(new Dataset(data.getName(), null));
			ti.setExpanded(true);
			treetableview.getRoot().getChildren().add(ti);
			for (Dataset dataset : data.getData().values()) {
				TreeItem treeItem = new TreeItem(dataset);
				ti.getChildren().add(treeItem);
			}
		} else {
			Data data = allData.get(_msg);
			data.getData().get("tms").setValue(System.currentTimeMillis());

		}

	}


	class Data {

		private String name;
		private Map<String,Dataset> data = new HashMap<String,Dataset>();

		public Data(String name, ObservableMap<String,Dataset> data) {
			this.name = name;
			this.data = data;
		}

		public Map<String,Dataset> getData() {
			return data;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	class Dataset {

		StringProperty str = new SimpleStringProperty();
		ObjectProperty<Number> value = new SimpleObjectProperty<>();

		public Dataset(String s, Number n) {
			str.set(s);
			value.set(n);
		}

		public Number getValue() {
			return value.get();
		}

		public ObjectProperty<Number> noProperty() {
			return value;
		}

		public void setValue(Number no) {
			this.value.set(no);
		}

		public String getStr() {
			return str.get();
		}

		public StringProperty strProperty() {
			return str;
		}

		public void setStr(String str) {
			this.str.set(str);
		}
	}







}
