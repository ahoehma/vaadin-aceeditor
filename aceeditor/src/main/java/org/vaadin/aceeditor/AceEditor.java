package org.vaadin.aceeditor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vaadin.aceeditor.client.AceAnnotation;
import org.vaadin.aceeditor.client.AceAnnotation.MarkerAnnotation;
import org.vaadin.aceeditor.client.AceAnnotation.RowAnnotation;
import org.vaadin.aceeditor.client.AceDoc;
import org.vaadin.aceeditor.client.AceEditorClientRpc;
import org.vaadin.aceeditor.client.AceEditorServerRpc;
import org.vaadin.aceeditor.client.AceEditorState;
import org.vaadin.aceeditor.client.AceMarker;
import org.vaadin.aceeditor.client.AceMarker.OnTextChange;
import org.vaadin.aceeditor.client.AceMarker.Type;
import org.vaadin.aceeditor.client.AceRange;
import org.vaadin.aceeditor.client.TransportDiff;
import org.vaadin.aceeditor.client.TransportDoc.TransportRange;
import org.vaadin.aceeditor.client.Util;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.BlurNotifier;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.FieldEvents.FocusNotifier;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.FieldEvents.TextChangeNotifier;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.util.ReflectTools;

/**
 *
 * AceEditor wraps an Ace code editor inside a TextField-like Vaadin component.
 *
 */
@SuppressWarnings("serial")
@JavaScript({"client/js/ace/ace.js", "client/js/ace/ext-searchbox.js",
    "client/js/diff_match_patch.js"})
@StyleSheet("client/css/ace-gwt.css")
public class AceEditor extends AbstractField<String>
    implements
      BlurNotifier,
      FocusNotifier,
      TextChangeNotifier {

  public static class DiffEvent extends Event {
    public static String EVENT_ID = "aceeditor-diff";
    private final ServerSideDocDiff diff;

    public DiffEvent(final AceEditor ed, final ServerSideDocDiff diff) {
      super(ed);
      this.diff = diff;
    }

    public ServerSideDocDiff getDiff() {
      return diff;
    }
  }

  public interface DiffListener extends Serializable {
    public static final Method diffMethod = ReflectTools.findMethod(
        DiffListener.class, "diff", DiffEvent.class);

    public void diff(DiffEvent e);
  }

  public static class SelectionChangeEvent extends Event {
    public static String EVENT_ID = "aceeditor-selection";
    private final TextRange selection;

    public SelectionChangeEvent(final AceEditor ed) {
      super(ed);
      selection = ed.getSelection();
    }

    public TextRange getSelection() {
      return selection;
    }
  }

  public interface SelectionChangeListener extends Serializable {
    public static final Method selectionChangedMethod = ReflectTools
        .findMethod(SelectionChangeListener.class, "selectionChanged",
            SelectionChangeEvent.class);

    public void selectionChanged(SelectionChangeEvent e);
  }

  public static class TextChangeEventImpl extends TextChangeEvent {
    private final TextRange selection;
    private final String text;

    private TextChangeEventImpl(final AceEditor ace, final String text,
        final AceRange selection) {
      super(ace);
      this.text = text;
      this.selection = ace.getSelection();
    }

    @Override
    public AbstractTextField getComponent() {
      return (AbstractTextField) super.getComponent();
    }

    @Override
    public int getCursorPosition() {
      return selection.getEnd();
    }

    @Override
    public String getText() {
      return text;
    }
  }

  // By default, using the version 1.1.9 of Ace from GitHub via rawgit.com.
  // It's recommended to host the Ace files yourself as described in README.
  private static final String DEFAULT_ACE_PATH = "//cdn.rawgit.com/ajaxorg/ace-builds/e3ccd2c654cf45ee41ffb09d0e7fa5b40cf91a8f/src-min-noconflict";

  private AceDoc doc = new AceDoc();

  private boolean isFiringTextChangeEvent;

  private boolean latestFocus = false;
  private long latestMarkerId = 0L;

  private static final Logger logger = Logger.getLogger(AceEditor.class
      .getName());

  private boolean onRoundtrip = false;

  private final AceEditorServerRpc rpc = new AceEditorServerRpc() {
    @Override
    public void changed(final TransportDiff diff, final TransportRange selection,
        final boolean focused) {
      clientChanged(diff, selection, focused);
    }

    @Override
    public void changedDelayed(final TransportDiff diff,
        final TransportRange selection, final boolean focused) {
      clientChanged(diff, selection, focused);
    }
  };

  private TextRange selection = new TextRange("", 0, 0, 0, 0);
  // {startPos,endPos} or {startRow,startCol,endRow,endCol}
  private Integer[] selectionToClient = null;
  private AceDoc shadow = new AceDoc();

  {
    logger.setLevel(Level.WARNING);
  }

  public AceEditor() {
    super();
    setWidth("300px");
    setHeight("200px");

    setModePath(DEFAULT_ACE_PATH);
    setThemePath(DEFAULT_ACE_PATH);
    setWorkerPath(DEFAULT_ACE_PATH);

    registerRpc(rpc);
  }

  public void addDiffListener(final DiffListener listener) {
    addListener(DiffEvent.EVENT_ID, DiffEvent.class, listener,
        DiffListener.diffMethod);
  }

  @Override
  public void addFocusListener(final FocusListener listener) {
    addListener(FocusEvent.EVENT_ID, FocusEvent.class, listener,
        FocusListener.focusMethod);
    getState().listenToFocusChanges = true;
  }

  @Override
  public void addBlurListener(final BlurListener listener) {
    addListener(BlurEvent.EVENT_ID, BlurEvent.class, listener,
        BlurListener.blurMethod);
    getState().listenToFocusChanges = true;
  }

  @Override
  @Deprecated
  public void addListener(final BlurListener listener) {
    addBlurListener(listener);
  }

  @Override
  @Deprecated
  public void addListener(final FocusListener listener) {
    addFocusListener(listener);
  }

  @Override
  @Deprecated
  public void addListener(final TextChangeListener listener) {
    addTextChangeListener(listener);
  }

  /**
   * Adds an ace marker. The id of the marker must be unique within this editor.
   *
   * @param marker
   * @return marker id
   */
  public String addMarker(final AceMarker marker) {
    doc = doc.withAdditionalMarker(marker);
    markAsDirty();
    return marker.getMarkerId();
  }

  /**
   * Adds an ace marker with a generated id. The id is unique within this editor.
   *
   * @param range
   * @param cssClass
   * @param type
   * @param inFront
   * @param onChange
   * @return marker id
   */
  public String addMarker(final AceRange range, final String cssClass, final Type type,
      final boolean inFront, final OnTextChange onChange) {
    return addMarker(new AceMarker(newMarkerId(), range, cssClass, type,
        inFront, onChange));
  }

  public void addMarkerAnnotation(final AceAnnotation ann, final AceMarker marker) {
    addMarkerAnnotation(ann, marker.getMarkerId());
  }

  public void addMarkerAnnotation(final AceAnnotation ann, final String markerId) {
    doc = doc.withAdditionalMarkerAnnotation(new MarkerAnnotation(markerId,
        ann));
    markAsDirty();
  }

  public void addRowAnnotation(final AceAnnotation ann, final int row) {
    doc = doc.withAdditionalRowAnnotation(new RowAnnotation(row, ann));
    markAsDirty();
  }

  public void addSelectionChangeListener(final SelectionChangeListener listener) {
    addListener(SelectionChangeEvent.EVENT_ID, SelectionChangeEvent.class,
        listener, SelectionChangeListener.selectionChangedMethod);
    getState().listenToSelectionChanges = true;
  }

  @Override
  public void addTextChangeListener(final TextChangeListener listener) {
    addListener(TextChangeListener.EVENT_ID, TextChangeEvent.class,
        listener, TextChangeListener.EVENT_METHOD);
  }

  @Override
  public void beforeClientResponse(final boolean initial) {
    super.beforeClientResponse(initial);
    if (initial) {
      getState().initialValue = doc.asTransport();
      shadow = doc;
    } else if (onRoundtrip) {
      final ServerSideDocDiff diff = ServerSideDocDiff.diff(shadow, doc);
      shadow = doc;
      final TransportDiff td = diff.asTransport();
      getRpcProxy(AceEditorClientRpc.class).diff(td);

      onRoundtrip = false;
    } else if (true /* TODO !shadow.equals(doc) */) {
      getRpcProxy(AceEditorClientRpc.class).changedOnServer();
    }

    if (selectionToClient != null) {
      // {startPos,endPos}
      if (selectionToClient.length == 2) {
        final AceRange r = AceRange.fromPositions(selectionToClient[0],
            selectionToClient[1], doc.getText());
        getState().selection = r.asTransport();
      }
      // {startRow,startCol,endRow,endCol}
      else if (selectionToClient.length == 4) {
        final TransportRange tr = new TransportRange(selectionToClient[0],
            selectionToClient[1], selectionToClient[2],
            selectionToClient[3]);
        getState().selection = tr;
      }
      selectionToClient = null;
    }
  }

  public void clearMarkerAnnotations() {
    final Set<MarkerAnnotation> manns = Collections.emptySet();
    doc = doc.withMarkerAnnotations(manns);
    markAsDirty();
  }

  public void clearMarkers() {
    doc = doc.withoutMarkers();
    markAsDirty();
  }

  public void clearRowAnnotations() {
    final Set<RowAnnotation> ranns = Collections.emptySet();
    doc = doc.withRowAnnotations(ranns);
    markAsDirty();
  }

  public int getCursorPosition() {
    return selection.getEnd();
  }

  public AceDoc getDoc() {
    return doc;
  }

  public TextRange getSelection() {
    return selection;
  }

  @Override
  public Class<? extends String> getType() {
    return String.class;
  }

  public void removeDiffListener(final DiffListener listener) {
    removeListener(DiffEvent.EVENT_ID, DiffEvent.class, listener);
  }

  @Override
  public void removeFocusListener(final FocusListener listener) {
    removeListener(FocusEvent.EVENT_ID, FocusEvent.class, listener);
    getState().listenToFocusChanges = !getListeners(FocusEvent.class)
        .isEmpty() || !getListeners(BlurEvent.class).isEmpty();
  }

  @Override
  public void removeBlurListener(final BlurListener listener) {
    removeListener(BlurEvent.EVENT_ID, BlurEvent.class, listener);
    getState().listenToFocusChanges = !getListeners(FocusEvent.class)
        .isEmpty() || !getListeners(BlurEvent.class).isEmpty();
  }

  @Override
  @Deprecated
  public void removeListener(final BlurListener listener) {
    removeBlurListener(listener);
  }

  @Override
  @Deprecated
  public void removeListener(final FocusListener listener) {
    removeFocusListener(listener);
  }

  @Override
  @Deprecated
  public void removeListener(final TextChangeListener listener) {
    removeTextChangeListener(listener);
  }

  public void removeMarker(final AceMarker marker) {
    removeMarker(marker.getMarkerId());
  }

  public void removeMarker(final String markerId) {
    doc = doc.withoutMarker(markerId);
    markAsDirty();
  }

  public void removeSelectionChangeListener(final SelectionChangeListener listener) {
    removeListener(SelectionChangeEvent.EVENT_ID,
        SelectionChangeEvent.class, listener);
    getState().listenToSelectionChanges = !getListeners(
        SelectionChangeEvent.class).isEmpty();
  }

  @Override
  public void removeTextChangeListener(final TextChangeListener listener) {
    removeListener(TextChangeListener.EVENT_ID, TextChangeEvent.class,
        listener);
  }

  public void setBasePath(final String path) {
    setAceConfig("basePath", path);
  }

  /**
   * Sets the cursor position to be pos characters from the beginning of the text.
   *
   * @param pos
   */
  public void setCursorPosition(final int pos) {
    setSelection(pos, pos);
  }

  /**
   * Sets the cursor on the given row and column.
   *
   * @param row
   *          starting from 0
   * @param col
   *          starting from 0
   */
  public void setCursorRowCol(final int row, final int col) {
    setSelectionRowCol(row, col, row, col);
  }

  public void setDoc(final AceDoc doc) {
    if (this.doc.equals(doc)) {
      return;
    }
    this.doc = doc;
    final boolean wasReadOnly = isReadOnly();
    setReadOnly(false);
    setValue(doc.getText());
    setReadOnly(wasReadOnly);
    markAsDirty();
  }

  public void setMode(final AceMode mode) {
    getState().mode = mode.toString();
  }

  public void setMode(final String mode) {
    getState().mode = mode;
  }

  public void setModePath(final String path) {
    setAceConfig("modePath", path);
  }

  /**
   * Sets the selection to be between characters [start,end).
   *
   * The cursor will be at the end.
   *
   * @param start
   * @param end
   */
  // TODO
  public void setSelection(final int start, final int end) {
    setSelectionToClient(new Integer[] {start, end});
    setInternalSelection(new TextRange(getInternalValue(), start, end));
  }

  /**
   * Sets the selection to be between the given (startRow,startCol) and (endRow, endCol).
   *
   * The cursor will be at the end.
   *
   * @param startRow
   *          starting from 0
   * @param startCol
   *          starting from 0
   * @param endRow
   *          starting from 0
   * @param endCol
   *          starting from 0
   */
  public void setSelectionRowCol(final int startRow, final int startCol, final int endRow,
      final int endCol) {
    setSelectionToClient(new Integer[] {startRow, startCol, endRow, endCol});
    setInternalSelection(new TextRange(doc.getText(), startRow, startCol,
        endRow, endCol));
  }

  /**
   * Sets the mode how the TextField triggers {@link TextChangeEvent}s.
   *
   * @param inputEventMode
   *          the new mode
   *
   * @see TextChangeEventMode
   */
  public void setTextChangeEventMode(final TextChangeEventMode inputEventMode) {
    getState().changeMode = inputEventMode.toString();
  }

  /**
   * The text change timeout modifies how often text change events are communicated to the application when {@link #setTextChangeEventMode}
   * is {@link TextChangeEventMode#LAZY} or {@link TextChangeEventMode#TIMEOUT}.
   *
   *
   * @param timeoutMs
   *          the timeout in milliseconds
   */
  public void setTextChangeTimeout(final int timeoutMs) {
    getState().changeTimeout = timeoutMs;

  }

  /**
   * Scrolls to the given row. First row is 0.
   *
   */
  public void scrollToRow(final int row) {
    getState().scrollToRow = row;
  }

  /**
   * Scrolls the to the given position (characters from the start of the file).
   *
   */
  public void scrollToPosition(final int pos) {
    final int[] rowcol = Util.lineColFromCursorPos(getInternalValue(), pos, 0);
    scrollToRow(rowcol[0]);
  }

  public void setTheme(final AceTheme theme) {
    getState().theme = theme.toString();
  }

  public void setTheme(final String theme) {
    getState().theme = theme;
  }

  public void setThemePath(final String path) {
    setAceConfig("themePath", path);
  }

  public void setUseWorker(final boolean useWorker) {
    getState().useWorker = useWorker;
  }

  public void setWordWrap(final boolean ww) {
    getState().wordwrap = ww;
  }

  public void setShowGutter(final boolean showGutter) {
    getState().showGutter = showGutter;
  }

  public boolean isShowGutter() {
    return getState(false).showGutter;
  }

  public void setShowPrintMargin(final boolean showPrintMargin) {
    getState().showPrintMargin = showPrintMargin;
  }

  public boolean isShowPrintMargin() {
    return getState(false).showPrintMargin;
  }

  /**
   * @since 0.8.14.1
   */
  public void setPrintMarginColumn(final int printMargin) {
    getState().printMarginColumn = printMargin;
  }

  /**
   * @since 0.8.14.1
   */
  public int getPrintMarginColumn() {
    return getState(false).printMarginColumn;
  }

  public void setHighlightActiveLine(final boolean highlightActiveLine) {
    getState().highlightActiveLine = highlightActiveLine;
  }

  public boolean isHighlightActiveLine() {
    return getState(false).highlightActiveLine;
  }

  public void setWorkerPath(final String path) {
    setAceConfig("workerPath", path);
  }

  /**
   * Use "auto" if you want to detect font size from CSS
   *
   * @param size
   *          auto or font size
   */
  public void setFontSize(final String size) {
    getState().fontSize = size;
  }

  public String getFontSize() {
    return getState(false).fontSize;
  }

  public void setHighlightSelectedWord(final boolean highlightSelectedWord) {
    getState().highlightSelectedWord = highlightSelectedWord;
  }

  public boolean isHighlightSelectedWord() {
    return getState(false).highlightSelectedWord;
  }

  public void setShowInvisibles(final boolean showInvisibles) {
    getState().showInvisibles = showInvisibles;
  }

  public boolean isShowInvisibles() {
    return getState(false).showInvisibles;
  }

  public void setDisplayIndentGuides(final boolean displayIndentGuides) {
    getState().displayIndentGuides = displayIndentGuides;
  }

  public boolean isDisplayIndentGuides() {
    return getState(false).displayIndentGuides;
  }

  public void setTabSize(final int size) {
    getState().tabSize = size;
  }

  public void setUseSoftTabs(final boolean softTabs) {
    getState().softTabs = softTabs;
  }

  protected void clientChanged(final TransportDiff diff, final TransportRange selection,
      final boolean focused) {
    diffFromClient(diff);
    selectionFromClient(selection);
    if (latestFocus != focused) {
      latestFocus = focused;
      if (focused) {
        fireFocus();
      } else {
        fireBlur();
      }
    }

    clearStateFromServerToClient();
  }

  // Here we clear the selection etc. we sent earlier.
  // The client has already received the values,
  // and we must clear them at some point to not keep
  // setting the same selection etc. over and over.
  // TODO: this is a bit messy...
  private void clearStateFromServerToClient() {
    getState().selection = null;
    getState().scrollToRow = -1;
  }

  @Override
  protected AceEditorState getState() {
    return (AceEditorState) super.getState();
  }

  @Override
  protected AceEditorState getState(final boolean markAsDirty) {
    return (AceEditorState) super.getState(markAsDirty);
  }

  @Override
  protected void setInternalValue(final String newValue) {
    super.setInternalValue(newValue);
    doc = doc.withText(newValue);
  }

  private void diffFromClient(final TransportDiff d) {
    final String previousText = doc.getText();
    final ServerSideDocDiff diff = ServerSideDocDiff.fromTransportDiff(d);
    shadow = diff.applyTo(shadow);
    doc = diff.applyTo(doc);
    if (!TextUtils.equals(doc.getText(), previousText)) {
      setValue(doc.getText(), true);
      fireTextChangeEvent();
    }
    if (!diff.isIdentity()) {
      fireDiff(diff);
    }
    onRoundtrip = true;
    markAsDirty();
  }

  private void fireBlur() {
    fireEvent(new BlurEvent(this));
  }

  private void fireDiff(final ServerSideDocDiff diff) {
    fireEvent(new DiffEvent(this, diff));
  }

  private void fireFocus() {
    fireEvent(new FocusEvent(this));
  }

  private void fireSelectionChanged() {
    fireEvent(new SelectionChangeEvent(this));
  }

  private void fireTextChangeEvent() {
    if (!isFiringTextChangeEvent) {
      isFiringTextChangeEvent = true;
      try {
        fireEvent(new TextChangeEventImpl(this, getInternalValue(),
            selection));
      } finally {
        isFiringTextChangeEvent = false;
      }
    }
  }

  private String newMarkerId() {
    return "m" + (++latestMarkerId);
  }

  private void selectionFromClient(final TransportRange sel) {
    final TextRange newSel = new TextRange(doc.getText(),
        AceRange.fromTransport(sel));
    if (newSel.equals(selection)) {
      return;
    }
    setInternalSelection(newSel);
    fireSelectionChanged();
  }

  private void setAceConfig(final String key, final String value) {
    getState().config.put(key, value);
  }

  private void setInternalSelection(final TextRange selection) {
    this.selection = selection;
    getState().selection = selection.asTransport();
  }

  private void setSelectionToClient(final Integer[] stc) {
    selectionToClient = stc;
    markAsDirty();
  }

}
