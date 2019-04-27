package org.vaadin.aceeditor.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.vaadin.aceeditor.client.AceAnnotation.MarkerAnnotation;
import org.vaadin.aceeditor.client.AceAnnotation.RowAnnotation;
import org.vaadin.aceeditor.client.AceMarker.OnTextChange;
import org.vaadin.aceeditor.client.ClientSideDocDiff.Adjuster;
import org.vaadin.aceeditor.client.gwt.GwtAceAnnotation;
import org.vaadin.aceeditor.client.gwt.GwtAceChangeCursorHandler;
import org.vaadin.aceeditor.client.gwt.GwtAceChangeEvent;
import org.vaadin.aceeditor.client.gwt.GwtAceChangeEvent.Data.Action;
import org.vaadin.aceeditor.client.gwt.GwtAceChangeHandler;
import org.vaadin.aceeditor.client.gwt.GwtAceChangeSelectionHandler;
import org.vaadin.aceeditor.client.gwt.GwtAceEditor;
import org.vaadin.aceeditor.client.gwt.GwtAceEvent;
import org.vaadin.aceeditor.client.gwt.GwtAceFocusBlurHandler;
import org.vaadin.aceeditor.client.gwt.GwtAceKeyboardHandler;
import org.vaadin.aceeditor.client.gwt.GwtAcePosition;
import org.vaadin.aceeditor.client.gwt.GwtAceRange;
import org.vaadin.aceeditor.client.gwt.GwtAceSelection;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FocusWidget;

/**
 * A {@link com.google.gwt.user.client.ui.Widget} containing {@link org.vaadin.aceeditor.client.gwt.GwtAceEditor}
 */
public class AceEditorWidget extends FocusWidget
    implements
      GwtAceChangeHandler,
      GwtAceFocusBlurHandler,
      GwtAceChangeSelectionHandler,
      GwtAceChangeCursorHandler {

  public interface TextChangeListener {
    public void changed();
  }
  public interface SelectionChangeListener {
    public void selectionChanged();
  }

  public interface FocusChangeListener {
    public void focusChanged(boolean focused);
  }

  protected LinkedList<TextChangeListener> changeListeners = new LinkedList<>();
  public void addTextChangeListener(final TextChangeListener li) {
    changeListeners.add(li);
  }
  public void removeTextChangeListener(final TextChangeListener li) {
    changeListeners.remove(li);
  }

  protected LinkedList<SelectionChangeListener> selChangeListeners = new LinkedList<>();
  public void addSelectionChangeListener(final SelectionChangeListener li) {
    selChangeListeners.add(li);
  }
  public void removeSelectionChangeListener(final SelectionChangeListener li) {
    selChangeListeners.remove(li);
  }

  protected FocusChangeListener focusChangeListener;
  public void setFocusChangeListener(final FocusChangeListener li) {
    focusChangeListener = li;
  }

  protected class MarkerInEditor {
    protected AceMarker marker;
    protected String clientId;
    protected MarkerInEditor(final AceMarker marker, final String clientId) {
      this.marker = marker;
      this.clientId = clientId;
    }
  }

  protected class AnnotationInEditor {
    protected int row;
    protected AceAnnotation ann;
    protected String markerId;
    protected AnnotationInEditor(final AceAnnotation ann, final String markerId) {
      this.ann = ann;
      this.markerId = markerId;
    }
  }

  protected GwtAceEditor editor;

  protected String editorId;

  protected static int idCounter = 0;

  protected String text = "";
  protected boolean enabled = true;
  protected boolean readOnly = false;
  protected boolean propertyReadOnly = false;
  protected boolean focused;
  protected AceRange selection = new AceRange(0, 0, 0, 0);

  // key: marker markerId
  protected Map<String, MarkerInEditor> markersInEditor = Collections.emptyMap();

  protected Set<RowAnnotation> rowAnnsInEditor = Collections.emptySet();
  protected Set<AnnotationInEditor> markerAnnsInEditor = Collections.emptySet();

  protected Map<Integer, AceRange> invisibleMarkers = new HashMap<>();
  protected int latestInvisibleMarkerId = 0;

  protected boolean ignoreEditorEvents = false;

  protected Set<MarkerAnnotation> markerAnnotations = Collections.emptySet();
  protected Set<RowAnnotation> rowAnnotations = Collections.emptySet();

  protected GwtAceKeyboardHandler keyboardHandler;

  protected AceDoc doc;

  protected static String nextId() {
    return "_AceEditorWidget_" + (++idCounter);
  }

  public AceEditorWidget() {
    super(DOM.createDiv());
    editorId = nextId();
    this.setStylePrimaryName("AceEditorWidget");

  }

  public boolean isInitialized() {
    return editor != null;
  }

  public void initialize() {
    editor = GwtAceEditor.create(getElement(), editorId);
    editor.addChangeHandler(this);
    editor.addFocusListener(this);
    editor.addChangeSelectionHandler(this);
    editor.addChangeCursorHandler(this);
    if (keyboardHandler != null) {
      editor.setKeyboardHandler(keyboardHandler);
    }
  }

  public void setKeyboardHandler(final GwtAceKeyboardHandler handler) {
    keyboardHandler = handler;
    if (isInitialized()) {
      editor.setKeyboardHandler(handler);
    }
  }

  @Override
  public void setWidth(final String w) {
    super.setWidth(w);
    if (editor != null) {
      editor.resize();
    }
  }

  @Override
  public void setHeight(final String h) {
    super.setHeight(h);
    if (editor != null) {
      editor.resize();
    }
  }

  public void setWordwrap(final boolean wrap) {
    if (isInitialized()) {
      editor.setUseWrapMode(wrap);
    }
  }

  public void setShowGutter(final boolean showGutter) {
    if (isInitialized()) {
      editor.setShowGutter(showGutter);
    }
  }

  public void setShowPrintMargin(final boolean showPrintMargin) {
    if (isInitialized()) {
      editor.setShowPrintMargin(showPrintMargin);
    }
  }

  /**
   * @since 0.8.14.1
   */
  public void setPrintMarginColumn(final int printMargin) {
    if (isInitialized()) {
      editor.setPrintMarginColumn(printMargin);
    }
  }

  public void setHighlightActiveLineEnabled(final boolean highlightActiveLine) {
    if (isInitialized()) {
      editor.setHighlightActiveLineEnabled(highlightActiveLine);
    }
  }

  public void setDisplayIndentGuides(final boolean displayIndentGuides) {
    if (isInitialized()) {
      editor.setDisplayIndentGuides(displayIndentGuides);
    }
  }

  public void setUseSoftTabs(final boolean softTabs) {
    if (isInitialized()) {
      editor.setUseSoftTabs(softTabs);
    }
  }

  public void setTabSize(final int tabSize) {
    if (isInitialized()) {
      editor.setTabSize(tabSize);
    }
  }

  protected void setText(final String text) {
    if (!isInitialized() || this.text.equals(text)) {
      return;
    }
    final AceRange oldSelection = selection;
    final Adjuster adjuster = new Adjuster(this.text, text);
    adjustInvisibleMarkersOnTextChange(adjuster);
    this.text = text;
    doc = null;
    ignoreEditorEvents = true;
    final double wasAtRow = editor.getScrollTopRow();
    editor.setText(text);
    final AceRange adjSel = adjuster.adjust(oldSelection);
    setSelection(adjSel, true);
    editor.scrollToRow(wasAtRow);
    ignoreEditorEvents = false;
  }

  protected void adjustInvisibleMarkersOnTextChange(final Adjuster adjuster) {
    final HashMap<Integer, AceRange> ims = new HashMap<>(invisibleMarkers.size());
    for (final Entry<Integer, AceRange> e : invisibleMarkers.entrySet()) {
      ims.put(e.getKey(), adjuster.adjust(e.getValue()));
    }
    invisibleMarkers = ims;
  }

  public void setSelection(final AceRange s) {
    setSelection(s, false);
  }

  protected void setSelection(final AceRange s, final boolean force) {
    if (!isInitialized()) {
      return;
    }
    if (s.equals(selection) && !force) {
      return;
    }

    selection = s;

    final int r1 = s.getStartRow();
    final int c1 = s.getStartCol();
    final int r2 = s.getEndRow();
    final int c2 = s.getEndCol();
    final boolean backwards = r1 > r2 || r1 == r2 && c1 > c2;
    GwtAceRange range;
    if (backwards) {
      range = GwtAceRange.create(r2, c2, r1, c1);
    } else {
      range = GwtAceRange.create(r1, c1, r2, c2);
    }
    editor.setSelection(range, backwards);
  }

  public void setMode(final String mode) {
    if (!isInitialized()) {
      return;
    }
    editor.setMode(mode);
  }

  public void setTheme(final String theme) {
    if (!isInitialized()) {
      return;
    }
    editor.setTheme(theme);
  }

  public void setFontSize(final String fontSize) {
    if (!isInitialized()) {
      return;
    }
    editor.setFontSize(fontSize);
  }

  public void setHighlightSelectedWord(final boolean highlightSelectedWord) {
    if (!isInitialized()) {
      return;
    }
    editor.setHighlightSelectedWord(highlightSelectedWord);
  }

  protected void setMarkers(final Map<String, AceMarker> markers) {
    if (!isInitialized()) {
      return;
    }

    final HashMap<String, MarkerInEditor> newMarkers = new HashMap<>();
    for (final Entry<String, AceMarker> e : markers.entrySet()) {
      final String mId = e.getKey();
      final AceMarker m = e.getValue();
      MarkerInEditor existing = markersInEditor.get(mId);
      if (existing != null) {
        editor.removeMarker(existing.clientId);
      }
      final String type = m.getType() == AceMarker.Type.cursor
          ? "text"
          : m.getType() == AceMarker.Type.cursorRow ? "line" : m.getType().toString();
      final String clientId = editor.addMarker(convertRange(m.getRange()), m.getCssClass(), type, m.isInFront());
      existing = new MarkerInEditor(m, clientId);
      newMarkers.put(mId, existing);
    }

    for (final MarkerInEditor hehe : markersInEditor.values()) {
      if (!newMarkers.containsKey(hehe.marker.getMarkerId())) {
        editor.removeMarker(hehe.clientId);
      }
    }

    markersInEditor = newMarkers;
    adjustMarkerAnnotations();
  }

  protected void adjustMarkerAnnotations() {
    boolean changed = false;
    for (final AnnotationInEditor aie : markerAnnsInEditor) {
      final int row = rowOfMarker(aie.markerId);
      if (row != -1 && row != aie.row) {
        aie.row = row;
        changed = true;
      }
    }
    if (changed) {
      setAnnotationsToEditor();
    }
  }

  protected void setAnnotations(final Set<MarkerAnnotation> manns, final Set<RowAnnotation> ranns) {
    if (!isInitialized()) {
      return;
    }
    if (manns != null) {
      markerAnnotations = manns;
      markerAnnsInEditor = createAIEfromMA(manns);
    }
    if (ranns != null) {
      rowAnnotations = ranns;
      rowAnnsInEditor = ranns;
    }
    setAnnotationsToEditor();
  }

  protected void setAnnotationsToEditor() {
    final JsArray<GwtAceAnnotation> arr = GwtAceAnnotation.createEmptyArray();

    final JsArray<GwtAceAnnotation> existing = editor.getAnnotations();

    for (int i = 0; i < existing.length(); ++i) {
      final GwtAceAnnotation ann = existing.get(i);
      if (!ann.isVaadinAceEditorAnnotation()) {
        arr.push(ann);
      }
    }

    for (final AnnotationInEditor maie : markerAnnsInEditor) {
      final GwtAceAnnotation jsAnn = GwtAceAnnotation.create(maie.ann.getType().toString(), maie.ann.getMessage(), maie.row);
      arr.push(jsAnn);
    }
    for (final RowAnnotation ra : rowAnnsInEditor) {
      final AceAnnotation a = ra.getAnnotation();
      final GwtAceAnnotation jsAnn = GwtAceAnnotation.create(a.getType().toString(), a.getMessage(), ra.getRow());
      arr.push(jsAnn);
    }
    editor.setAnnotations(arr);
  }

  protected Set<AnnotationInEditor> createAIEfromMA(
      final Set<MarkerAnnotation> anns) {
    final Set<AnnotationInEditor> adjusted = new HashSet<>();
    for (final MarkerAnnotation a : anns) {
      final int row = rowOfMarker(a.getMarkerId());
      if (row != -1) {
        final AnnotationInEditor maie = new AnnotationInEditor(a.getAnnotation(), a.getMarkerId());
        maie.row = row;
        adjusted.add(maie);
      }
    }
    return adjusted;
  }

  protected int rowOfMarker(final String markerId) {
    final MarkerInEditor cm = markersInEditor.get(markerId);
    if (cm == null) {
      return -1;
    }
    return cm.marker.getRange().getStartRow();
  }

  @Override
  public void onChange(final GwtAceChangeEvent e) {
    if (ignoreEditorEvents) {
      return;
    }
    final String newText = editor.getText();
    if (newText.equals(text)) {
      return;
    }

    // TODO: do we do too much work here?
    // most of the time the editor doesn't have any markers nor annotations...

    adjustMarkers(e);
    adjustInvisibleMarkers(e);
    adjustMarkerAnnotations();
    text = newText;
    doc = null;
    fireTextChanged();
  }

  public void fireTextChanged() {
    for (final TextChangeListener li : changeListeners) {
      li.changed();
    }
  }

  protected void adjustMarkers(final GwtAceChangeEvent e) {
    final Action act = e.getData().getAction();
    final GwtAceRange range = e.getData().getRange();
    final Set<MarkerInEditor> moved = new HashSet<>();
    final Set<MarkerInEditor> removed = new HashSet<>();

    if (act == Action.insertLines || act == Action.insertText) {
      for (final MarkerInEditor cm : markersInEditor.values()) {
        if (cm.marker.getOnChange() == OnTextChange.ADJUST) {
          AceRange newRange = moveMarkerOnInsert(cm.marker.getRange(), range);
          if (newRange != null) {
            newRange = cursorMarkerSanityCheck(cm.marker, newRange);
            cm.marker = cm.marker.withNewPosition(newRange);
            if (markerIsValid(cm.marker)) {
              moved.add(cm);
            } else {
              removed.add(cm);
            }
          }
        } else if (cm.marker.getOnChange() == OnTextChange.REMOVE) {
          removed.add(cm);
        }
      }
    } else if (act == Action.removeLines || act == Action.removeText) {
      for (final MarkerInEditor cm : markersInEditor.values()) {
        if (cm.marker.getOnChange() == OnTextChange.ADJUST) {
          AceRange newRange = moveMarkerOnRemove(cm.marker.getRange(), range);
          if (newRange != null) {
            newRange = cursorMarkerSanityCheck(cm.marker, newRange);
            cm.marker = cm.marker.withNewPosition(newRange);
            if (markerIsValid(cm.marker)) {
              moved.add(cm);
            } else {
              removed.add(cm);
            }
          }
        } else if (cm.marker.getOnChange() == OnTextChange.REMOVE) {
          removed.add(cm);
        }
      }
    }

    removeMarkers(removed);
    updateMarkers(moved);
  }

  private AceRange cursorMarkerSanityCheck(final AceMarker m, final AceRange r) {
    if (m.getType() == AceMarker.Type.cursorRow && r.getEndRow() > r.getStartRow() + 1) {
      return new AceRange(r.getStartRow(), 0, r.getStartRow() + 1, 0);
    }
    if (m.getType() == AceMarker.Type.cursor &&
        (r.getStartRow() != r.getEndRow() || r.getEndCol() > r.getStartCol() + 1)) {
      return new AceRange(r.getEndRow(), r.getEndCol(), r.getEndRow(), r.getEndCol() + 1);
    }

    return r;
  }
  protected void adjustInvisibleMarkers(final GwtAceChangeEvent event) {
    final Action act = event.getData().getAction();
    final GwtAceRange range = event.getData().getRange();
    final HashMap<Integer, AceRange> newMap = new HashMap<>();
    if (act == Action.insertLines || act == Action.insertText) {
      for (final Entry<Integer, AceRange> e : invisibleMarkers.entrySet()) {
        final AceRange newRange = moveMarkerOnInsert(e.getValue(), range);
        newMap.put(e.getKey(), newRange == null ? e.getValue() : newRange);
      }
    } else if (act == Action.removeLines || act == Action.removeText) {
      for (final Entry<Integer, AceRange> e : invisibleMarkers.entrySet()) {
        final AceRange newRange = moveMarkerOnRemove(e.getValue(), range);
        newMap.put(e.getKey(), newRange == null ? e.getValue() : newRange);
      }
    }
    invisibleMarkers = newMap;
  }

  protected static boolean markerIsValid(final AceMarker marker) {
    final AceRange r = marker.getRange();
    return !r.isZeroLength() && !r.isBackwards() && r.getStartRow() >= 0 && r.getStartCol() >= 0 && r.getEndCol() >= 0; // no need to check
                                                                                                                        // endrow
  }

  protected static AceRange moveMarkerOnInsert(final AceRange mr, final GwtAceRange range) {
    final int startRow = range.getStart().getRow();
    final int startCol = range.getStart().getColumn();
    final int dRow = range.getEnd().getRow() - startRow;
    final int dCol = range.getEnd().getColumn() - startCol;

    if (dRow == 0 && dCol == 0) {
      return null;
    }

    if (range.getStart().getRow() > mr.getEndRow()) {
      return null;
    }

    final boolean aboveMarkerStart = startRow < mr.getStartRow();
    final boolean beforeMarkerStartOnRow = startRow == mr.getStartRow() && startCol < mr.getStartCol(); // < or <=
    final boolean aboveMarkerEnd = startRow < mr.getEndRow();
    final boolean beforeMarkerEndOnRow = startRow == mr.getEndRow() && startCol <= mr.getEndCol(); // < or <=

    int row1 = mr.getStartRow();
    int col1 = mr.getStartCol();
    if (aboveMarkerStart) {
      row1 += dRow;
    } else if (beforeMarkerStartOnRow) {
      row1 += dRow;
      col1 += dCol;
    }

    int row2 = mr.getEndRow();
    int col2 = mr.getEndCol();
    if (aboveMarkerEnd) {
      row2 += dRow;
    } else if (beforeMarkerEndOnRow) {
      row2 += dRow;
      col2 += dCol;
    }

    return new AceRange(row1, col1, row2, col2);
  }

  protected static AceRange moveMarkerOnRemove(final AceRange mr, final GwtAceRange range) {
    int[] p1 = overlapping(range, mr.getStartRow(), mr.getStartCol());
    boolean changed = false;
    if (p1 == null) {
      p1 = new int[] {mr.getStartRow(), mr.getStartCol()};
    } else {
      changed = true;
    }

    int[] p2 = overlapping(range, mr.getEndRow(), mr.getEndCol());
    if (p2 == null) {
      p2 = new int[] {mr.getEndRow(), mr.getEndCol()};
    } else {
      changed = true;
    }

    return changed ? new AceRange(p1[0], p1[1], p2[0], p2[1]) : null;
  }

  protected static int[] overlapping(final GwtAceRange range, final int row, final int col) {
    final GwtAcePosition start = range.getStart();

    if (start.getRow() > row || start.getRow() == row && start.getColumn() >= col) {
      return null;
    }

    final GwtAcePosition end = range.getEnd();

    if (end.getRow() < row) {
      final int dRow = end.getRow() - start.getRow();
      return new int[] {row - dRow, col};
    }
    if (end.getRow() == row && end.getColumn() < col) {
      final int dRow = end.getRow() - start.getRow();
      final int dCol = end.getColumn() - start.getColumn();
      return new int[] {row - dRow, col - dCol};
    }
    return new int[] {start.getRow(), start.getColumn()};
  }

  protected void removeMarkers(final Set<MarkerInEditor> removed) {
    for (final MarkerInEditor cm : removed) {
      editor.removeMarker(cm.clientId);
      markersInEditor.remove(cm.marker.getMarkerId());
    }
  }

  protected void updateMarkers(final Set<MarkerInEditor> moved) {
    for (final MarkerInEditor cm : moved) {
      editor.removeMarker(cm.clientId);
      final AceMarker m = cm.marker;
      cm.clientId = editor.addMarker(convertRange(m.getRange()), m.getCssClass(), m.getType().toString(), m.isInFront());
    }

  }

  public String getText() {
    return text;
  }

  @Override
  public void setEnabled(final boolean enabled) {
    if (!isInitialized()) {
      return;
    }
    this.enabled = enabled;
    updateEditorReadOnlyState();
  }

  public void setPropertyReadOnly(final boolean propertyReadOnly) {
    if (!isInitialized()) {
      return;
    }
    this.propertyReadOnly = propertyReadOnly;
    updateEditorReadOnlyState();
  }

  public void setReadOnly(final boolean readOnly) {
    if (!isInitialized()) {
      return;
    }
    this.readOnly = readOnly;
    updateEditorReadOnlyState();
  }

  private void updateEditorReadOnlyState() {
    editor.setReadOnly(readOnly || propertyReadOnly || !enabled);
  }

  public void setShowInvisibles(final boolean showInvisibles) {
    editor.setShowInvisibles(showInvisibles);
  }

  protected static AceRange convertSelection(final GwtAceSelection selection) {
    final GwtAcePosition start = selection.getRange().getStart();
    final GwtAcePosition end = selection.getRange().getEnd();
    if (selection.isBackwards()) {
      return new AceRange(end.getRow(), end.getColumn(), start.getRow(),
          start.getColumn());
    } else {
      return new AceRange(start.getRow(), start.getColumn(),
          end.getRow(), end.getColumn());
    }

  }

  public AceRange getSelection() {
    return selection;
  }

  @Override
  public void onFocus(final GwtAceEvent e) {
    if (focused) {
      return;
    }
    focused = true;
    if (focusChangeListener != null) {
      focusChangeListener.focusChanged(true);
    }
  }

  @Override
  public void onBlur(final GwtAceEvent e) {
    if (!focused) {
      return;
    }
    focused = false;
    if (focusChangeListener != null) {
      focusChangeListener.focusChanged(false);
    }
  }

  @Override
  public void onChangeSelection(final GwtAceEvent e) {
    selectionChanged();
  }

  @Override
  public void onChangeCursor(final GwtAceEvent e) {
    selectionChanged();
  }

  protected void selectionChanged() {
    if (ignoreEditorEvents) {
      return;
    }
    final AceRange sel = convertSelection(editor.getSelection());
    if (!sel.equals(selection)) {
      selection = sel;
      for (final SelectionChangeListener li : selChangeListeners) {
        li.selectionChanged();
      }
    }
  }

  public void setUseWorker(final boolean use) {
    if (!isInitialized()) {
      return;
    }
    editor.setUseWorker(use);
  }

  @Override
  public void setFocus(final boolean focused) {
    super.setFocus(focused);
    if (focused) {
      editor.focus();
    } else {
      editor.blur();
    }
    // Waiting for the event from editor to update 'focused'.
  }

  public boolean isFocused() {
    return focused;
  }

  protected GwtAceRange convertRange(final AceRange r) {
    final int r1 = r.getStartRow();
    final int c1 = r.getStartCol();
    final int r2 = r.getEndRow();
    final int c2 = r.getEndCol();
    final boolean backwards = r1 > r2 || r1 == r2 && c1 > c2;
    if (backwards) {
      return GwtAceRange.create(r2, c2, r1, c1);
    } else {
      return GwtAceRange.create(r1, c1, r2, c2);
    }
  }

  protected Map<String, AceMarker> getMarkers() {
    final HashMap<String, AceMarker> markers = new HashMap<>();
    for (final MarkerInEditor cm : markersInEditor.values()) {
      markers.put(cm.marker.getMarkerId(), cm.marker);
    }
    return markers;
  }

  public void resize() {
    if (editor != null) {
      editor.resize();
    }
  }

  public AceDoc getDoc() {
    if (doc == null) {
      doc = new AceDoc(getText(), getMarkers(), getRowAnnotations(), getMarkerAnnotations());
    }
    return doc;
  }

  public void scrollToRow(final int row) {
    editor.scrollToRow(row);
  }

  protected Set<MarkerAnnotation> getMarkerAnnotations() {
    return markerAnnotations;
  }

  protected Set<RowAnnotation> getRowAnnotations() {
    return rowAnnotations;
  }

  public void setDoc(final AceDoc doc) {
    if (doc.equals(this.doc)) {
      return;
    }

    setText(doc.getText());

    // Too much work is done in the case there
    // are no markers or annotations, which is probably most of the time...
    // TODO: optimize

    setMarkers(doc.getMarkers());
    setAnnotations(doc.getMarkerAnnotations(), doc.getRowAnnotations());
    this.doc = doc;
  }

  public int[] getCursorCoords() {
    final JsArrayInteger cc = editor.getCursorCoords();
    return new int[] {cc.get(0), cc.get(1)};
  }

  public int addInvisibleMarker(final AceRange range) {
    final int id = ++latestInvisibleMarkerId;
    invisibleMarkers.put(id, range);
    return id;
  }

  public void removeInvisibleMarker(final int id) {
    invisibleMarkers.remove(id);
  }

  public AceRange getInvisibleMarker(final int id) {
    return invisibleMarkers.get(id);
  }

  public void setTextAndAdjust(final String text) {
    if (this.text.equals(text)) {
      return;
    }

    final HashMap<String, AceMarker> newMarkers = adjustMarkersOnTextChange(this.text, text);
    setText(text);
    if (newMarkers != null) {
      setMarkers(newMarkers);
    }
  }

  protected HashMap<String, AceMarker> adjustMarkersOnTextChange(final String text1, final String text2) {
    final Map<String, AceMarker> ms = getMarkers();
    if (ms.isEmpty()) {
      return null;
    }
    final HashMap<String, AceMarker> newMarkers = new HashMap<>();
    final Adjuster adjuster = new Adjuster(text1, text2);
    boolean adjusted = false;
    for (final Entry<String, AceMarker> e : ms.entrySet()) {
      if (e.getValue().getOnChange() == OnTextChange.ADJUST) {
        final AceMarker m1 = e.getValue();
        final AceMarker m2 = m1.withNewPosition(adjuster.adjust(m1.getRange()));
        newMarkers.put(e.getKey(), m2);
        adjusted = true;
      } else {
        newMarkers.put(e.getKey(), e.getValue());
      }
    }
    if (!adjusted) {
      return null;
    }
    return newMarkers;
  }

  public void removeContentsOfInvisibleMarker(final int imId) {
    final AceRange r = getInvisibleMarker(imId);
    if (r == null || r.isZeroLength()) {
      return;
    }
    final String newText = Util.replaceContents(r, text, "");
    setTextAndAdjust(newText);
  }

}
