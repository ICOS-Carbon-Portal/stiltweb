import React, { Component } from 'react';

export default class Dropdown extends Component{
	constructor(props){
		super(props);

		this.state = {
			dropdownOpen: false,
			hoverIdx: undefined
		};

		this.outsideClickHandler = this.handleOutsideClick.bind(this);
		this.hoverHandler = this.handleHover.bind(this);
		document.addEventListener('click', this.outsideClickHandler, false);
	}

	handleOutsideClick(e){
		if (!this.node.contains(e.target) && this.state.dropdownOpen) {
			this.setState({dropdownOpen: false});
		}
	}

	handleHover(idx){
		this.setState({hoverIdx: idx});
	}

	onDropdownClick(){
		this.setState({dropdownOpen: !this.state.dropdownOpen});
	}

	onDropDownItemClick(selectedIdx){
		const props = this.props;

		if (!props.itemClickAction)
			return;

		this.setState({dropdownOpen: !this.state.dropdownOpen});

		const currentBtnLbl = toStringValue(props.selectedValue, props.presenter) || props.buttonLbl;
		const selectedOpt = props.availableValues[selectedIdx];
		const newBtnLbl = toStringValue(selectedOpt, props.presenter) || props.buttonLbl;

		if (currentBtnLbl !== newBtnLbl)
			this.props.itemClickAction(selectedOpt);
	}

	componentWillUnmount(){
		document.removeEventListener('click', this.outsideClickHandler, false);
	}

	render(){
		const {dropdownOpen} = this.state;
		const props = this.props;

		const availableValues = getAvailableValues(props.sort, props.availableValues, props.presenter);
		const selectOptions = availableValues.map(v => toStringValue(v, props.presenter));
		const buttonLbl = toStringValue(props.selectedValue, props.presenter) || props.buttonLbl;
		const selectedIsICOS = props.selectedValue === null ? false : props.selectedValue.isICOS;
		const rootStyle = Object.assign({display: 'inline-block', marginBottom: 10}, props.style);
		const menuCls = dropdownOpen ? 'dropdown-menu overflow-scroll show' : 'dropdown-menu overflow-scroll';

		return (
			<span ref={div => this.node = div} className="dropdown" style={rootStyle}>{
				<Button
					clickAction={this.onDropdownClick.bind(this)}
					buttonLbl={buttonLbl}
					isICOS={selectedIsICOS}
				/>
			}

				<ul className={menuCls} style={{maxHeight: 400}}>
					{
						selectOptions.map((opt, idx) =>
							<li
								style={{padding: '2px 10px', cursor:'pointer', whiteSpace:'nowrap'}}
								className="dropdown-item"
								key={'ddl' + idx}
								onClick={this.onDropDownItemClick.bind(this, idx)}>
								<StationMarker isICOS={availableValues[idx].isICOS} />
								{opt}
							</li>
						)
					}</ul>
			</span>
		);
	}
}

const toStringValue = (value, presenter) => presenter ? presenter(value) : value;

const getAvailableValues = (sort, availableValues, presenter) =>
	sort
		? (availableValues || []).sort((v1, v2) => {
			const s1 = toStringValue(v1, presenter);
			const s2 = toStringValue(v2, presenter);
			return s1 > s2 ? 1 : s1 < s2 ? -1 : 0;
		})
		: (availableValues || []);

const StationMarker = ({isICOS, showSpace = true}) => {
	if (isICOS) {
		const src = "https://static.icos-cp.eu/images/cp_dots.png";
		const style = {height: 8, top: -2, position: 'relative', marginRight: 7};
		return <img src={src} style={style} title="ICOS Data" alt="Carbon Portal dots" />;
	}

	if (showSpace)
		return <span style={{marginRight: 40}} />;

	return null;
};

const Button = ({ clickAction, isICOS, buttonLbl = 'Select option' }) => {
	return (
		<button className="btn dropdown-toggle bg-white text-dark" style={{borderColor:'#ced4da'}} onClick={clickAction}>
			<StationMarker isICOS={isICOS} showSpace={false} /><span>{buttonLbl}</span> <span className="caret" />
		</button>
	);
};
