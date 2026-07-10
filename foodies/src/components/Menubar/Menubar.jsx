import React, { useContext } from "react";
import './Menubar.css'
import { assets } from "../../assets/assets";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { StoreContext } from "../../context/StoreContext";
const Menubar = () => {
   const {quantities,token,setToken, setQuantities, role, setRole } =
      useContext(StoreContext);
    const uniqueItemsInCart = Object.values(quantities).filter(
      (qty) => qty > 0
    ).length;
const navigate = useNavigate();
const isAdmin = role === "ADMIN";

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    setToken("");
    setRole("");
    setQuantities({});
    navigate("/");
  };

  const navLinkClass = ({ isActive }) =>
    isActive ? "nav-link fw-bold active" : "nav-link";

  return (
    <nav className="navbar navbar-expand-lg navbar-light bg-light">
  <div className="container">
    <Link to="/"><img src={assets.logo} alt="" className='mx-4' height={48} width={48}/></Link>
    <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
      <span className="navbar-toggler-icon"></span>
    </button>
    <div className="collapse navbar-collapse" id="navbarSupportedContent">
      <ul className="navbar-nav me-auto mb-2 mb-lg-0">
        <li className="nav-item">
          <NavLink className={navLinkClass} to="/" end>Home</NavLink>
        </li>
        <li className="nav-item">
          <NavLink className={navLinkClass} to="/explore">Explore</NavLink>
        </li>
        <li className="nav-item">
          <NavLink className={navLinkClass} to="/contact">Contact us</NavLink>
        </li>
        {isAdmin && (
          <>
            <li className="nav-item">
              <NavLink className={navLinkClass} to="/admin/foods">Manage Foods</NavLink>
            </li>
            <li className="nav-item">
              <NavLink className={navLinkClass} to="/admin/orders">Manage Orders</NavLink>
            </li>
          </>
        )}
      </ul>
     <div className="d-flex align-items-center gap-4">
      <Link to={`/cart`}>
        <div className='position-relative'>
            <img src={assets.cart} alt="" height={32} width={32} className='position-relative'/>
             <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-warning">{uniqueItemsInCart}</span>
        </div>
        </Link>
            {!token ? (
                      <>
                        <button className="btn btn-outline-primary btn-sm" onClick={() => navigate("/login")}>Login</button>
                        <button className="btn btn-outline-success btn-sm" onClick={() => navigate("/register")}>Register</button>
                        <button className="btn btn-outline-dark btn-sm" onClick={() => navigate("/admin/login")}>Admin</button>
                      </>
                    ) : (
                      <div className="dropdown text-end">
                        <a href="#" className="d-block link-body-emphasis text-decoration-none dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
                          <img src={assets.profile} alt="" width={32} height={32} className="rounded-circle"/>
                        </a>
                        <ul className="dropdown-menu text-small">
                          {!isAdmin && (
                            <li className="dropdown-item" onClick={() => navigate("/myorders")}>Orders</li>
                          )}
                          <li className="dropdown-item" onClick={logout}>Logout</li>
                        </ul>
                      </div>
                    )}
    </div>
    </div>
  </div>
</nav>
  )
}

export default Menubar
