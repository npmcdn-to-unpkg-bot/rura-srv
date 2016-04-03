import {AudioContainer} from '../containers/AudioContainer';
import {RadioPlayer} from './RadioPlayer';
import {ExtraAreaContainer} from '../containers/ExtraAreaContainer';

export const Radio = ({isPlaying, isExtraVisible}) => (
  <div className="radio">
    <AudioContainer />
    <RadioPlayer isPlaying={isPlaying} />
    <ExtraAreaContainer isExtraVisible={isExtraVisible} />
    <img src="//radio.ruranobe.ru" className="radio__mpegurl-loader" />
  </div>
);