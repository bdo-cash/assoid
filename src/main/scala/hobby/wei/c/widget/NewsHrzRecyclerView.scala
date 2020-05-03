package hobby.wei.c.widget

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import hobby.chenai.nakam.lang.TypeBring.AsIs
import hobby.wei.c.util.UiUtils
import hobby.wei.c.widget.NewsHrzRecyclerView.CyclicScrollAction

/**
  * @author Wei.Chou
  * @version 1.0, 28/02/2020
  */
class NewsHrzRecyclerView(context: Context, attrs: AttributeSet, defStyleAttr: Int) extends RecyclerView(context, attrs, defStyleAttr) {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)

  addItemDecoration(new MyItemDecoration(context))

  class MyItemDecoration(val context: Context) extends ItemDecoration {
    override def getItemOffsets(outRect: Rect, itemPosition: Int, parent: RecyclerView) {
      super.getItemOffsets(outRect, itemPosition, parent)
      // for smoothScrollToPosition(i--)
      //            if (itemPosition != 0) {
      //                outRect.left = UiUtils.dp2pxSize(context, 16f)
      //            }
      // for smoothScrollToPosition(i++)
      if (itemPosition != Option(parent.getAdapter).fold(1) {
        _.getItemCount
      } - 1) {
        outRect.right = UiUtils.dp2pxSize(context, 16f)
      }
    }
  }

  private def getCurrViewPosition(endX: Float): Int = {
    getChildAt(if (endX - downX < 0) getChildCount - 1 else 0)
      .getLayoutParams.as[RecyclerView.LayoutParams].getViewLayoutPosition
  }

  private var downX = 0f
  private var bool = false
  override def dispatchTouchEvent(ev: MotionEvent): Boolean = {
    ev.getAction match {
      case MotionEvent.ACTION_DOWN =>
        stopCyclicScroll()
        if ((getChildAt(0).getLeft < getPaddingStart) &&
          (getChildAt(getChildCount - 1).getRight > getRight - getPaddingEnd)
        ) {
          getParent.requestDisallowInterceptTouchEvent(true)
          bool = true
        } else {
          downX = ev.getX
          bool = false
        }
      case MotionEvent.ACTION_MOVE =>
        if (!bool) {
          if (((ev.getX - downX) > 0 && getChildAt(0).getLeft >= getPaddingStart)
            || ((ev.getX - downX) < 0 && getChildAt(getChildCount - 1).getRight <= getRight - getPaddingEnd)
          ) {
            getParent.requestDisallowInterceptTouchEvent(false)
          } else {
            getParent.requestDisallowInterceptTouchEvent(true)
            bool = true
          }
        }
      case MotionEvent.ACTION_UP =>
        startCyclicScroll(getCurrViewPosition(ev.getX))
      case MotionEvent.ACTION_CANCEL =>
        startCyclicScroll(getCurrViewPosition(ev.getX))
    }
    super.dispatchTouchEvent(ev) || bool
  }

  // var scrollDirection = true // true for currPos++, false for currPos--
  var alignToPosition = true

  def allowCyclicScroll(value: Boolean) {
    cyclicScrollAction.cyclicScroll = value
    if (value) startCyclicScroll() else stopCyclicScroll()
  }

  def allowCyclicScroll = cyclicScrollAction.cyclicScroll

  private def startCyclicScroll(position: Int = -1) {
    if (position != -1) cyclicScrollAction.currPos = position
    if (alignToPosition) cyclicScrollAction.run()
  }

  private def stopCyclicScroll() {
    myhandler.removeCallbacks(cyclicScrollAction)
  }

  private lazy val cyclicScrollAction = {
    new CyclicScrollAction(this, myhandler)
  }
  private lazy val myhandler = {
    Option(getHandler).fold(new Handler()) { h => h }
  }
}
object NewsHrzRecyclerView {
  class CyclicScrollAction(private val recycler: RecyclerView, private val handler: Handler) extends Runnable {
    var currPos = 0
    var cyclicScroll = false

    override def run() {
      if (currPos >= Option(recycler.getAdapter).fold(0) {
        _.getItemCount
      }) {
        currPos = 0
        recycler.scrollToPosition(0)
      } else {
        currPos += 1
        recycler.smoothScrollToPosition(currPos)
      }
      if (cyclicScroll) handler.postDelayed(this, 6000)
    }
  }
}