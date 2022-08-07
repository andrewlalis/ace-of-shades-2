package nl.andrewl.aos2_launcher.view;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.Collection;
import java.util.function.Function;

public class ElementList<T, V extends Node> {
	private final Pane container;

	private final ObjectProperty<T> selectedElement = new SimpleObjectProperty<>(null);
	private final ObservableList<T> elements = FXCollections.observableArrayList();
	private final Class<V> elementViewType;
	private final Function<V, T> viewElementMapper;

	public ElementList(
			Pane container,
			Function<T, V> elementViewMapper,
			Class<V> elementViewType,
			Function<V, T> viewElementMapper
	) {
		this.container = container;
		this.elementViewType = elementViewType;
		this.viewElementMapper = viewElementMapper;
		BindingUtil.mapContent(container.getChildren(), elements, element -> {
			V view = elementViewMapper.apply(element);
			view.getStyleClass().add("element-list-item");
			return view;
		});
		container.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClick);
	}

	@SuppressWarnings("unchecked")
	private void handleMouseClick(MouseEvent event) {
		Node target = (Node) event.getTarget();
		while (target != null) {
			if (target.getClass().equals(elementViewType)) {
				V elementView = (V) target;
				T targetElement = viewElementMapper.apply(elementView);
				if (event.isControlDown()) {
					if (selectedElement.get() == null) {
						selectElement(targetElement);
					} else {
						selectElement(null);
					}
				} else {
					selectElement(targetElement);
				}
				return; // Exit since we found a valid target.
			}
			target = target.getParent();
		}
		selectElement(null);
	}

	public void selectElement(T element) {
		if (element != null && !elements.contains(element)) return;
		selectedElement.set(element);
		updateSelectedPseudoClass();
	}

	@SuppressWarnings("unchecked")
	private void updateSelectedPseudoClass() {
		PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");
		for (var node : container.getChildren()) {
			if (!node.getClass().equals(elementViewType)) continue;
			V view = (V) node;
			T thisElement = viewElementMapper.apply(view);
			view.pseudoClassStateChanged(selectedClass, thisElement.equals(selectedElement.get()));
		}
	}

	public T getSelectedElement() {
		return selectedElement.get();
	}

	public ObjectProperty<T> selectedElementProperty() {
		return selectedElement;
	}

	public ObservableList<T> getElements() {
		return elements;
	}

	public void clear() {
		elements.clear();
		selectElement(null);
	}

	public void add(T element) {
		elements.add(element);
	}

	public void addAll(Collection<T> newElements) {
		elements.addAll(newElements);
	}

	public void remove(T element) {
		elements.remove(element);
		if (element != null && element.equals(selectedElement.get())) {
			selectElement(null);
		}
	}
}
