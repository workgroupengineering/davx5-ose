package at.bitfire.davdroid.ui.intro

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import at.bitfire.davdroid.R
import at.bitfire.davdroid.settings.SettingsManager
import kotlinx.android.synthetic.main.intro_welcome.*


class WelcomeFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val  v = inflater.inflate(R.layout.intro_welcome, container, false)

        // TODO animate
        /*val logo: ImageView = v.findViewById(R.id.logo)
        val logoBackground: ImageView = v.findViewById(R.id.logoBackground)
        val yourdata: TextView = v.findViewById(R.id.yourDataYourChoice)
        val takecontrol: TextView = v.findViewById(R.id.takeControl)*/

        /*logo.apply {
            animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.fadein)
            animation.startOffset = 0
            animation.start()
        }
        logoBackground?.apply {
            animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.fadein)
            animation.startOffset = 0
            animation.start()
        }
        yourDataYourChoice.apply {
            animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.fadein)
            animation.startOffset = 1000
            animation.start()
        }
        takeControl.apply {
            animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.fadein)
            animation.startOffset = 2000
            animation.start()
        }*/

        return v
    }


    class Factory : IIntroFragmentFactory {

        override fun shouldBeShown(context: Context, settingsManager: SettingsManager) = IIntroFragmentFactory.ShowMode.SHOW_NOT_ALONE

        override fun create() = WelcomeFragment()

    }

}