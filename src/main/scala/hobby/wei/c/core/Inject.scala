package hobby.wei.c.core

import android.app.Fragment
import android.os.Bundle
import android.view.{LayoutInflater, ViewGroup}
import hobby.wei.c.anno.inject.Injector

/**
  * @author Chenakam (chenai.nakam@gmail.com)
  * @version 1.0, 17/11/2017
  */
object Inject {
  trait Acty extends AbsActy {
    override protected def onCreate(savedInstanceState: Bundle): Unit = {
      super.onCreate(savedInstanceState)
      Injector.inject(this, classOf[AbsActy])
    }
  }

  trait Fragmt extends Fragment {
    override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
      super.onCreateView(inflater, container, savedInstanceState)
      val view = inflater.inflate(Injector.layoutID(getActivity, getClass), container, false)
      Injector.inject(this, view, classOf[Fragmt])
      view
    }
  }
}
